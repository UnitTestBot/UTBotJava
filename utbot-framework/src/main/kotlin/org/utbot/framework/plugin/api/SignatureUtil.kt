package org.utbot.framework.plugin.api

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

fun KFunction<*>.signature() =
    Signature(this.name, this.parameters.filter { it.kind == KParameter.Kind.VALUE }.map { it.type.javaType.typeName })

data class Signature(val name: String, val parameterTypes: List<String?>) {

    fun normalized() = this.copy(
        parameterTypes = parameterTypes.map {
            it?.replace("$", ".") // normalize names of nested classes
        }
    )
}