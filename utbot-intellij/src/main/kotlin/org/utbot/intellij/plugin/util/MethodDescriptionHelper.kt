package org.utbot.intellij.plugin.util

import com.intellij.psi.PsiMethod
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.utbot.framework.plugin.api.MethodDescription

fun MemberInfo.methodDescription(): MethodDescription =
    (this.member as PsiMethod).methodDescription()

// Note that rules for obtaining signature here should correlate with KFunction<*>.signature()
private fun PsiMethod.methodDescription() =
    MethodDescription(this.name, this.containingClass?.qualifiedName, this.parameterList.parameters.map {
        it.type.canonicalText
            .replace("...", "[]") //for PsiEllipsisType
            .replace(",", ", ") // to fix cases like Pair<String,String> -> Pair<String, String>
    })