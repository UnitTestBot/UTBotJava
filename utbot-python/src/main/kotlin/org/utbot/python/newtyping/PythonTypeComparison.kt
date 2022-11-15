package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.*

class PythonTypeWrapperForEqualityCheck(
    val type: Type,
    private val bounded: List<TypeParameter> = emptyList()
) {
    init {
        if (!type.isPythonType())
            error("Trying to create PythonTypeWrapperForComparison for non-Python type $type")
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

/*
class PythonSubtypeChecker(
    val left: PythonTypeWrapperForEqualityCheck,
    val right: PythonTypeWrapperForEqualityCheck
) {
    fun rightIsSubtypeOfLeft(): Boolean {
        return when (val leftMeta = left.type.meta as PythonTypeDescription) {
            is
        }
    }
}
 */

fun Type.isParameterBoundedTo(type: Type): Boolean =
    (this is TypeParameter) && (this.definedAt == type)

fun Type.getBoundedParametersIndexes(): List<Int> =
    parameters.mapIndexedNotNull { index, parameter ->
        if (parameter.isParameterBoundedTo(this)) {
            index
        } else {
            null
        }
    }

fun Type.getBoundedParameters(): List<TypeParameter> =
    parameters.mapNotNull { parameter ->
        if (parameter.isParameterBoundedTo(this)) {
            parameter as TypeParameter
        } else {
            null
        }
    }