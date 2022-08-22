package org.utbot.intellij.plugin.util

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.util.classMembers.MemberInfo
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

fun MemberInfo.signature(): Signature =
        (this.member as PsiMethod).signature()

private fun PsiMethod.signature() =
    Signature(this.name, this.parameterList.parameters.map {
        it.type.canonicalText
            .replace("...", "[]") //for PsiEllipsisType
            .replace(",", ", ") // to fix cases like Pair<String,String> -> Pair<String, String>
    })

fun KFunction<*>.signature() =
    Signature(this.name, this.parameters.filter { it.kind == KParameter.Kind.VALUE }.map { it.type.javaType.typeName })

data class Signature(val name: String, val parameterTypes: List<String?>) {

    fun normalized() = this.copy(
        parameterTypes = parameterTypes.map {
            it?.replace("$", ".") // normalize names of nested classes
        }
    )
}