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

fun typesAreEqual(left: Type, right: Type): Boolean {
    return PythonTypeWrapperForEqualityCheck(left) == PythonTypeWrapperForEqualityCheck(right)
}

const val MAX_RECURSION_DEPTH = 100

class PythonSubtypeChecker(
    val left: Type,
    val right: Type,
    private val pythonTypeStorage: PythonTypeStorage,
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

        if (typesAreEqual(left, pythonTypeStorage.pythonFloat) && typesAreEqual(right, pythonTypeStorage.pythonInt))
            return true

        // this is done to avoid possible infinite recursion
        // TODO: probably more accuracy is needed here
        if (assumingSubtypePairs.contains(Pair(leftWrapper, rightWrapper)) || assumingSubtypePairs.contains(
                Pair(
                    rightWrapper,
                    leftWrapper
                )
            )
        )
            return true

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

        if (rightMeta is PythonTypeAliasDescription)
            return PythonSubtypeChecker(
                left = left,
                right = rightMeta.getInterior(right),
                pythonTypeStorage,
                typeParameterCorrespondence, assumingSubtypePairs, recursionDepth + 1
            ).rightIsSubtypeOfLeft()

        return when (leftMeta) {
            is PythonAnyTypeDescription -> true
            is PythonTypeAliasDescription -> caseOfLeftTypeAlias(leftMeta)
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

    private fun caseOfLeftTypeAlias(leftMeta: PythonTypeAliasDescription): Boolean {
        return PythonSubtypeChecker(
            left = leftMeta.getInterior(left),
            right = right,
            pythonTypeStorage,
            typeParameterCorrespondence,
            nextAssumingSubtypePairs,
            recursionDepth + 1
        ).rightIsSubtypeOfLeft()
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
                            pythonTypeStorage,
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
                pythonTypeStorage,
                typeParameterCorrespondence,
                nextAssumingSubtypePairs,
                recursionDepth + 1
            ).rightIsSubtypeOfLeft()
        }
    }

    private fun caseOfLeftCompositeType(leftMeta: PythonConcreteCompositeTypeDescription): Boolean {
        if (left.isPythonObjectType())
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
                            rightArg.pythonDescription() is PythonAnyTypeDescription
                        )
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
                                    pythonTypeStorage,
                                    typeParameterCorrespondence,
                                    nextAssumingSubtypePairs,
                                    recursionDepth + 1
                                ).rightIsSubtypeOfLeft()
                            PythonTypeVarDescription.Variance.CONTRAVARIANT ->
                                PythonSubtypeChecker(
                                    left = rightArg,
                                    right = leftArg,
                                    pythonTypeStorage,
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
                            pythonTypeStorage,
                            typeParameterCorrespondence,
                            nextAssumingSubtypePairs,
                            recursionDepth + 1
                        ).rightIsSubtypeOfLeft()
                    }
                }
            }
            PythonTupleTypeDescription -> {
                if (!typesAreEqual(left.getOrigin(), pythonTypeStorage.pythonTuple) || left.hasBoundedParameters())
                    false
                else {
                    right.pythonAnnotationParameters().all {
                        PythonSubtypeChecker(
                            left = left.parameters.first(),
                            right = it,
                            pythonTypeStorage,
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
        if (!typeParameterCorrespondence.any { it.first == left })
            return true  // treat unbounded TypeVars like Any. TODO: here might occur false-positives
        return when (val rightMeta = right.pythonDescription()) {
            is PythonAnyTypeDescription -> true
            is PythonTypeVarDescription -> caseOfLeftAndRightTypeVar(leftMeta, rightMeta)
            else -> false
        }
    }

    private fun caseOfLeftUnion(leftMeta: PythonUnionTypeDescription): Boolean {
        val children = leftMeta.getAnnotationParameters(left)
        return children.any { childType ->
            PythonSubtypeChecker(
                left = childType,
                right = right,
                pythonTypeStorage,
                typeParameterCorrespondence,
                nextAssumingSubtypePairs,
                recursionDepth + 1
            ).rightIsSubtypeOfLeft()
        }
    }

    private fun caseOfLeftAndRightTypeVar(
        leftMeta: PythonTypeVarDescription,
        rightMeta: PythonTypeVarDescription
    ): Boolean {
        val leftParam = leftMeta.castToCompatibleTypeApi(left)
        val rightParam = rightMeta.castToCompatibleTypeApi(right)
        // TODO: more accurate case analysis
        return typeParameterCorrespondence.contains(Pair(leftParam, rightParam))
    }

    private fun caseOfLeftProtocol(leftMeta: PythonProtocolDescription): Boolean {
        val membersNotToCheck = listOf("__new__", "__init__")
        return leftMeta.protocolMemberNames.subtract(membersNotToCheck).all { protocolMemberName ->
            // there is a tricky case: importlib.metadata._meta.SimplePath
            val neededAttribute =
                left.getPythonAttributeByName(pythonTypeStorage, protocolMemberName) ?: return@all true
            val rightAttribute = right.getPythonAttributeByName(pythonTypeStorage, protocolMemberName) ?: return false
            val firstArgIsSelf = { description: PythonDefinitionDescription ->
                (description is PythonFuncItemDescription) && description.args.isNotEmpty() &&
                        description.args.first().isSelf
            }
            val description = neededAttribute.meta
            val skipFirstArgument =
                firstArgIsSelf(description) ||
                        ((description is PythonOverloadedFuncDefDescription) && description.items.any(firstArgIsSelf))
            PythonSubtypeChecker(
                left = neededAttribute.type,
                right = rightAttribute.type,
                pythonTypeStorage,
                typeParameterCorrespondence,
                nextAssumingSubtypePairs,
                recursionDepth + 1,
                skipFirstArgument
            ).rightIsSubtypeOfLeft()
        }
    }

    private fun caseOfLeftCallable(leftMeta: PythonCallableTypeDescription): Boolean {
        val rightCallAttributeAbstract = right.getPythonAttributeByName(pythonTypeStorage, "__call__")?.type
            ?: return false
        val leftAsFunctionType = leftMeta.castToCompatibleTypeApi(left)

        // TODO: more accurate work with argument binding?

        if (rightCallAttributeAbstract.pythonDescription() is PythonOverloadTypeDescription) {
            val variants = rightCallAttributeAbstract.parameters
            return variants.any { variant ->
                PythonSubtypeChecker(
                    left = left,
                    right = variant,
                    pythonTypeStorage,
                    typeParameterCorrespondence,
                    nextAssumingSubtypePairs,
                    recursionDepth + 1,
                    skipFirstArgument
                ).rightIsSubtypeOfLeft()
            }
        }

        val rightCallAttribute = rightCallAttributeAbstract as? FunctionType ?: return false

        if (rightCallAttribute.arguments.size != leftAsFunctionType.arguments.size)
            return false
        val leftBounded = leftAsFunctionType.getBoundedParameters()
        val rightBounded = rightCallAttribute.getBoundedParameters()

        // TODO: more accurate case analysis
        if (rightBounded.isNotEmpty() && leftBounded.size != rightBounded.size)
            return false

        var newLeftAsFunctionType = leftAsFunctionType

        // TODO: here might occur false-positives
        if (rightBounded.isEmpty() && leftBounded.isNotEmpty()) {
            val newLeft = DefaultSubstitutionProvider.substitute(left, leftBounded.associateWith { pythonAnyType })
            newLeftAsFunctionType = leftMeta.castToCompatibleTypeApi(newLeft)
        }

        val newCorrespondence = typeParameterCorrespondence +
                if (rightBounded.isNotEmpty()) (leftBounded zip rightBounded) else emptyList()

        var args = newLeftAsFunctionType.arguments zip rightCallAttribute.arguments
        if (skipFirstArgument)
            args = args.drop(1)

        return args.all { (leftArg, rightArg) ->
            PythonSubtypeChecker(
                left = rightArg,
                right = leftArg,
                pythonTypeStorage,
                reverse(newCorrespondence),
                nextAssumingSubtypePairs,
                recursionDepth + 1
            ).rightIsSubtypeOfLeft()
        } && PythonSubtypeChecker(
            left = newLeftAsFunctionType.returnValue,
            right = rightCallAttribute.returnValue,
            pythonTypeStorage,
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
                    && right.pythonDescription() is PythonCompositeTypeDescription
                )
                    assumingSubtypePairs +
                            listOf(
                                Pair(
                                    PythonTypeWrapperForEqualityCheck(left),
                                    PythonTypeWrapperForEqualityCheck(right)
                                )
                            )
                else
                    assumingSubtypePairs
            }

    companion object {
        fun checkIfRightIsSubtypeOfLeft(left: Type, right: Type, pythonTypeStorage: PythonTypeStorage): Boolean =
            PythonSubtypeChecker(
                left = left,
                right = right,
                pythonTypeStorage,
                emptyList(),
                emptyList(),
                0
            ).rightIsSubtypeOfLeft()
    }
}