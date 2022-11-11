package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.*

/*
class PythonTypeWrapperForComparison private constructor(
    val type: Type,
    private val bounded: List<TypeParameter> = emptyList()
) {
    init {
        if (!type.isPythonType())
            error("Trying to create PythonTypeWrapperForComparison not of Python type")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PythonTypeWrapperForComparison)
            return false
        val otherMeta = other.type.pythonDescription()
        val selfMeta = type.pythonDescription()
        when (selfMeta) {
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
                        otherMeta.name == type.name &&
                        equalParameters(other)
            }
            is PythonCallable -> {
                return other.type is PythonCallable &&
                        other.type.argumentKinds == type.argumentKinds &&
                        equalParameters(other) &&
                        equalChildren(
                            type.arguments + listOf(type.returnValue),
                            other.type.arguments + listOf(other.type.returnValue),
                            other
                        )
            }
            is StatefulType -> {
                return other.type is StatefulType &&
                        type.name == other.type.name &&
                        equalChildren(
                            type.members,
                            other.type.members,
                            other
                        )
            }
            is NamedType -> {
                return type.name == (other.type as? NamedType)?.name
            }
        }
    }

    override fun hashCode(): Int {
        return when (type) {
            is TypeParameter -> {
                val selfIndex = bounded.indexOf(type as? TypeParameter)
                if (selfIndex == -1)
                    type.hashCode()
                else
                    return selfIndex
            }
            is PythonCompositeTypeDescription -> {
                (listOf(type.name.hashCode()) + type.parameters.map {
                    getChildWrapper(it).hashCode()
                }).hashCode()
            }
            is PythonCallable -> {
                (type.arguments + listOf(type.returnValue)).map {
                    getChildWrapper(it).hashCode()
                }.hashCode()
            }
            is StatefulType -> {
                (listOf(type.name.hashCode()) + type.members.map {
                    getChildWrapper(it).hashCode()
                }).hashCode()
            }
            is NamedType -> {
                type.name.hashCode()
            }
            else -> error("Some Python type wasn't considered in hashCode() of PythonTypeWrapperForComparison")
        }
    }

    private fun equalChildren(
        selfChildren: List<Type>,
        otherChildren: List<Type>,
        other: PythonTypeWrapperForComparison
    ): Boolean {
        if (selfChildren.size != otherChildren.size)
            return false
        return (selfChildren zip otherChildren).all { (selfElem, otherElem) ->
            getChildWrapper(selfElem) == other.getChildWrapper(otherElem)
        }
    }

    private fun getChildWrapper(elem: Type): PythonTypeWrapperForComparison {
        return PythonTypeWrapperForComparison(elem, bounded + type.getBoundedParameters())
    }

    private fun equalParameters(other: PythonTypeWrapperForComparison): Boolean {
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
            PythonTypeWrapperForComparison(it.first, bounded + newEquivSelf) ==
                    PythonTypeWrapperForComparison(it.second, other.bounded + newEquivOther)
        }
    }
}

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
 */