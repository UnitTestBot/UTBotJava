package org.utbot.framework.plugin.api

import org.utbot.framework.plugin.api.util.declaringClazz
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

// Note that rules for obtaining signature here should correlate with PsiMethod.signature()
fun KFunction<*>.methodDescription() =
    MethodDescription(
        this.name,
        this.declaringClazz.name,
        this.parameters.filter { it.kind != KParameter.Kind.INSTANCE }.map { it.type.javaType.typeName }
    )

// Similar to MethodId, but significantly simplified -- used only to match methods from psi and their reflections
data class MethodDescription(val name: String, val containingClass: String?, val parameterTypes: List<String?>) {

    fun normalized() = this.copy(
        containingClass = containingClass?.replace("$", "."), // normalize names of nested classes
        parameterTypes = parameterTypes.map {
            it?.replace("$", ".")
        }
    )
}