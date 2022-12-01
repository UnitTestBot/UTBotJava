package org.utbot.python.newtyping.general

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

fun Type.hasBoundedParameters(): Boolean =
    parameters.any { it.isParameterBoundedTo(this) }

fun Type.getOrigin(): Type =
    if (this is TypeSubstitution) rawOrigin.getOrigin() else this