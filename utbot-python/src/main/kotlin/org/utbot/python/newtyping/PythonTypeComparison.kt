package org.utbot.python.newtyping

import mu.KotlinLogging
import org.utbot.python.newtyping.general.*

private val logger = KotlinLogging.logger {}

class PythonTypeWrapperForEqualityCheck(
    val type: Type,
    private val bounded: List<TypeParameter> = emptyList()
) {
    init {
        if (!type.isPythonType())
            error("Trying to create PythonTypeWrapperForEqualityCheck for non-Python type $type")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PythonTypeWrapperForEqualityCheck)
            return false
        val otherMeta = other.type.pythonDescription()
        when (val selfMeta = type.pythonDescription()) {
            is PythonTypeVarDescription -> {
                if (type == other.type as? TypeParameter)
                    return true
                val selfIndex = bounded.indexOf(type as? TypeParameter)
                if (selfIndex == -1)
                    return false
                val otherIndex = other.bounded.indexOf(other.type as? TypeParameter)
                return selfIndex == otherIndex
            }
            is PythonCompositeTypeDescription -> {
                return otherMeta is PythonCompositeTypeDescription &&
                        otherMeta.name == selfMeta.name &&
                        equalParameters(other)
            }
            is PythonCallableTypeDescription -> {
                if (otherMeta !is PythonCallableTypeDescription)
                    return false
                return selfMeta.argumentKinds == otherMeta.argumentKinds &&
                        equalParameters(other) &&
                        equalChildren(
                            selfMeta.getAnnotationParameters(type),
                            otherMeta.getAnnotationParameters(other.type),
                            other
                        )
            }
            is PythonSpecialAnnotation -> {
                if (otherMeta !is PythonSpecialAnnotation)
                    return false
                return selfMeta.name == otherMeta.name &&
                        equalChildren(
                            selfMeta.getAnnotationParameters(type),
                            otherMeta.getAnnotationParameters(other.type),
                            other
                        )
            }
        }
    }

    override fun hashCode(): Int {
        return when (val selfMeta = type.pythonDescription()) {
            is PythonTypeVarDescription -> {
                val selfIndex = bounded.indexOf(type as? TypeParameter)
                if (selfIndex == -1)
                    type.hashCode()
                else
                    return selfIndex
            }
            else -> {
                (listOf(selfMeta.name.hashCode()) + selfMeta.getAnnotationParameters(type).map {
                    getChildWrapper(it).hashCode()
                }).hashCode()
            }
        }
    }

    private fun equalChildren(
        selfChildren: List<Type>,
        otherChildren: List<Type>,
        other: PythonTypeWrapperForEqualityCheck
    ): Boolean {
        if (selfChildren.size != otherChildren.size)
            return false
        return (selfChildren zip otherChildren).all { (selfElem, otherElem) ->
            getChildWrapper(selfElem) == other.getChildWrapper(otherElem)
        }
    }

    private fun getChildWrapper(elem: Type): PythonTypeWrapperForEqualityCheck {
        return PythonTypeWrapperForEqualityCheck(elem, bounded + type.getBoundedParameters())
    }

    private fun equalParameters(other: PythonTypeWrapperForEqualityCheck): Boolean {
        if (type.parameters.size != other.type.parameters.size)
            return false

        val selfBoundedInd = type.getBoundedParametersIndexes()
        val otherBoundedInd = other.type.getBoundedParametersIndexes()

        if (selfBoundedInd != otherBoundedInd)
            return false

        // constraints for bounded parameters are not checked to avoid possible cyclic dependencies

        return (type.parameters zip other.type.parameters).all {
            val (newEquivSelf, newEquivOther) =
                if (it.first.isParameterBoundedTo(type))
                    Pair(listOf(it.first as TypeParameter), listOf(it.second as TypeParameter))
                else
                    Pair(emptyList(), emptyList())
            PythonTypeWrapperForEqualityCheck(it.first, bounded + newEquivSelf) ==
                    PythonTypeWrapperForEqualityCheck(it.second, other.bounded + newEquivOther)
        }
    }
}

const val MAX_RECURSION_DEPTH = 100

class PythonSubtypeChecker(
    val left: Type,
    val right: Type,
    private val typeParameterCorrespondence: List<Pair<Type, Type>>,
    private val assumingSubtypePairs: List<Pair<PythonTypeWrapperForEqualityCheck, PythonTypeWrapperForEqualityCheck>>,
    private val recursionDepth: Int,
    private val skipFirstArgument: Boolean = false
) {
    init {
        if (!left.isPythonType() || !right.isPythonType())
            error("Trying to create PythonSubtypeChecker for non-Python types $left, $right")
    }
    fun rightIsSubtypeOfLeft(): Boolean {
        val leftWrapper = PythonTypeWrapperForEqualityCheck(left)
        val rightWrapper = PythonTypeWrapperForEqualityCheck(right)
        if (leftWrapper == rightWrapper)
            return true

        // this is done to avoid possible infinite recursion
        if (assumingSubtypePairs.contains(Pair(leftWrapper, rightWrapper)))
            return true
        if (assumingSubtypePairs.contains(Pair(rightWrapper, leftWrapper)))
            return false

        val leftMeta = left.meta as PythonTypeDescription
        val rightMeta = right.meta as PythonTypeDescription

        // subtype checking is not supported for types with bounded parameters, except CallableType
        if (left.hasBoundedParameters() && leftMeta !is PythonCallableTypeDescription)
            return false
        if (right.hasBoundedParameters() && rightMeta !is PythonCallableTypeDescription)
            return false

        if (rightMeta is PythonAnyTypeDescription)
            return true

        // just in case
        if (recursionDepth >= MAX_RECURSION_DEPTH) {
            logger.warn("Recursion depth limit exceeded")
            return false
        }

        return when (leftMeta) {
            is PythonAnyTypeDescription -> true
            is PythonTypeVarDescription -> caseOfLeftTypeVar(leftMeta)
            is PythonProtocolDescription -> caseOfLeftProtocol(leftMeta)
            is PythonCallableTypeDescription -> caseOfLeftCallable(leftMeta)
            is PythonUnionTypeDescription -> caseOfLeftUnion(leftMeta)
            is PythonConcreteCompositeTypeDescription -> caseOfLeftCompositeType(leftMeta)
            is PythonNoneTypeDescription -> rightMeta is PythonNoneTypeDescription
            is PythonOverloadTypeDescription -> caseOfLeftOverload(leftMeta)
            is PythonTupleTypeDescription -> caseOfLeftTupleType(leftMeta)
        }
    }
    private fun caseOfLeftTupleType(leftMeta: PythonTupleTypeDescription): Boolean {
        return when (val rightMeta = right.pythonDescription()) {
            is PythonAnyTypeDescription -> true
            is PythonTupleTypeDescription -> {
                val leftAsCompositeType = leftMeta.castToCompatibleTypeApi(left)
                val rightAsCompositeType = rightMeta.castToCompatibleTypeApi(right)
                if (leftAsCompositeType.parameters.size != rightAsCompositeType.parameters.size)
                    false
                else {
                    (leftAsCompositeType.parameters zip rightAsCompositeType.parameters).all { (leftMember, rightMember) ->
                        PythonSubtypeChecker(
                            left = leftMember,
                            right = rightMember,
                            typeParameterCorrespondence,
                            nextAssumingSubtypePairs,
                            recursionDepth + 1
                        ).rightIsSubtypeOfLeft()
                    }
                }
            }
            else -> false
        }
    }
    private fun caseOfLeftOverload(leftMeta: PythonOverloadTypeDescription): Boolean {
        val leftAsStatefulType = leftMeta.castToCompatibleTypeApi(left)
        return leftAsStatefulType.parameters.all {
            PythonSubtypeChecker(
                left = it,
                right = right,
                typeParameterCorrespondence,
                nextAssumingSubtypePairs,
                recursionDepth + 1
            ).rightIsSubtypeOfLeft()
        }
    }
    private fun caseOfLeftCompositeType(leftMeta: PythonConcreteCompositeTypeDescription): Boolean {
        if (PythonTypeWrapperForEqualityCheck(left) == PythonTypeWrapperForEqualityCheck(BuiltinTypes.pythonObject))
            return true
        return when (val rightMeta = right.pythonDescription()) {
            is PythonAnyTypeDescription -> true
            is PythonConcreteCompositeTypeDescription -> {
                val rightAsCompositeType = rightMeta.castToCompatibleTypeApi(right)
                if (rightMeta.name == leftMeta.name) {
                    val origin = left.getOrigin()
                    (left.parameters zip right.parameters zip origin.parameters).all {
                        val (args, param) = it
                        val (leftArg, rightArg) = args
                        if (leftArg.pythonDescription() is PythonAnyTypeDescription ||
                            rightArg.pythonDescription() is PythonAnyTypeDescription)
                            return@all true
                        val typeVarDescription = param.pythonDescription()
                        if (typeVarDescription !is PythonTypeVarDescription)  // shouldn't be possible
                            return@all false
                        when (typeVarDescription.variance) {
                            PythonTypeVarDescription.Variance.INVARIANT -> false
                            PythonTypeVarDescription.Variance.COVARIANT ->
                                PythonSubtypeChecker(
                                    left = leftArg,
                                    right = rightArg,
                                    typeParameterCorrespondence,
                                    nextAssumingSubtypePairs,
                                    recursionDepth + 1
                                ).rightIsSubtypeOfLeft()
                            PythonTypeVarDescription.Variance.CONTRAVARIANT ->
                                PythonSubtypeChecker(
                                    left = rightArg,
                                    right = leftArg,
                                    reverse(typeParameterCorrespondence),
                                    nextAssumingSubtypePairs,
                                    recursionDepth + 1
                                ).rightIsSubtypeOfLeft()
                        }
                    }
                } else {
                    rightAsCompositeType.supertypes.any {
                        PythonSubtypeChecker(
                            left = left,
                            right = it,
                            typeParameterCorrespondence,
                            nextAssumingSubtypePairs,
                            recursionDepth + 1
                        ).rightIsSubtypeOfLeft()
                    }
                }
            }
            else -> false
        }
    }
    private fun caseOfLeftTypeVar(leftMeta: PythonTypeVarDescription): Boolean {
        // TODO: more accurate case analysis
        return when (val rightMeta = right.pythonDescription()) {
            is PythonAnyTypeDescription -> true
            is PythonTypeVarDescription -> caseOfLeftAndRightTypeVar(leftMeta, rightMeta)
            else -> false
        }
    }
    private fun caseOfLeftUnion(leftMeta: PythonUnionTypeDescription): Boolean {
        val children = leftMeta.getAnnotationParameters(left)
        return children.any {  childType ->
            PythonSubtypeChecker(
                left = childType,
                right = right,
                typeParameterCorrespondence,
                nextAssumingSubtypePairs,
                recursionDepth + 1
            ).rightIsSubtypeOfLeft()
        }
    }
    private fun caseOfLeftAndRightTypeVar(leftMeta: PythonTypeVarDescription, rightMeta: PythonTypeVarDescription): Boolean {
        val leftParam = leftMeta.castToCompatibleTypeApi(left)
        val rightParam = rightMeta.castToCompatibleTypeApi(right)
        // TODO: more accurate case analysis
        return typeParameterCorrespondence.contains(Pair(leftParam, rightParam))
    }
    private fun caseOfLeftProtocol(leftMeta: PythonProtocolDescription): Boolean {
        val membersNotToCheck = listOf("__new__", "__init__")
        return leftMeta.protocolMemberNames.subtract(membersNotToCheck).all { protocolMemberName ->
            val neededAttribute = left.getPythonAttributeByName(protocolMemberName)!!
            val rightAttribute = right.getPythonAttributeByName(protocolMemberName) ?: return false
            val description = neededAttribute.type.pythonDescription()
            val skipFirstArgument =
                (description is PythonCallableTypeDescription) && !description.isStaticMethod
            PythonSubtypeChecker(
                left = neededAttribute.type,
                right = rightAttribute.type,
                typeParameterCorrespondence,
                nextAssumingSubtypePairs,
                recursionDepth + 1,
                skipFirstArgument
            ).rightIsSubtypeOfLeft()
        }
    }
    private fun caseOfLeftCallable(leftMeta: PythonCallableTypeDescription): Boolean {
        val rightCallAttribute = right.getPythonAttributeByName("__call__")?.type as? FunctionType
            ?: return false
        val leftAsFunctionType = leftMeta.castToCompatibleTypeApi(left)
        // TODO: more accurate work with argument binding?
        if (rightCallAttribute.arguments.size != leftAsFunctionType.arguments.size)
            return false
        val leftBounded = leftAsFunctionType.getBoundedParameters()
        val rightBounded = rightCallAttribute.getBoundedParameters()

        // TODO: more accurate case analysis
        if (leftBounded.size != rightBounded.size)
            return false

        val newCorrespondence = typeParameterCorrespondence + (leftBounded zip rightBounded)

        var args = leftAsFunctionType.arguments zip rightCallAttribute.arguments
        if (skipFirstArgument)
            args = args.drop(1)

        return args.all { (leftArg, rightArg) ->
            PythonSubtypeChecker(
                left = rightArg,
                right = leftArg,
                reverse(newCorrespondence),
                nextAssumingSubtypePairs,
                recursionDepth + 1
            ).rightIsSubtypeOfLeft()
        } && PythonSubtypeChecker(
            left = leftAsFunctionType.returnValue,
            right = rightCallAttribute.returnValue,
            newCorrespondence,
            nextAssumingSubtypePairs,
            recursionDepth + 1
        ).rightIsSubtypeOfLeft()
    }

    private fun reverse(correspondence: List<Pair<Type, Type>>): List<Pair<Type, Type>> =
        correspondence.map { Pair(it.second, it.first) }

    private val nextAssumingSubtypePairs: List<Pair<PythonTypeWrapperForEqualityCheck, PythonTypeWrapperForEqualityCheck>>
        by lazy {
            if (left.pythonDescription() is PythonCompositeTypeDescription
                && right.pythonDescription() is PythonCompositeTypeDescription)
                assumingSubtypePairs +
                        listOf(Pair(PythonTypeWrapperForEqualityCheck(left), PythonTypeWrapperForEqualityCheck(right)))
            else
                assumingSubtypePairs
        }

    companion object {
        fun checkIfRightIsSubtypeOfLeft(left: Type, right: Type): Boolean =
            PythonSubtypeChecker(
                left = left,
                right = right,
                emptyList(),
                emptyList(),
                0
            ).rightIsSubtypeOfLeft()
    }
}