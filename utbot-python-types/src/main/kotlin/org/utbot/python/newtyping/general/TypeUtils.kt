package org.utbot.python.newtyping.general

fun UtType.isParameterBoundedTo(type: UtType): Boolean =
    (this is TypeParameter) && (this.definedAt == type)

fun UtType.getBoundedParametersIndexes(): List<Int> =
    parameters.mapIndexedNotNull { index, parameter ->
        if (parameter.isParameterBoundedTo(this)) {
            index
        } else {
            null
        }
    }

fun UtType.getBoundedParameters(): List<TypeParameter> =
    parameters.mapNotNull { parameter ->
        if (parameter.isParameterBoundedTo(this)) {
            parameter as TypeParameter
        } else {
            null
        }
    }

fun UtType.hasBoundedParameters(): Boolean =
    parameters.any { it.isParameterBoundedTo(this) }

fun UtType.getOrigin(): UtType =
    if (this is TypeSubstitution) rawOrigin.getOrigin() else this