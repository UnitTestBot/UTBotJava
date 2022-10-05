package org.utbot.intellij.plugin.util

import com.intellij.psi.PsiMethod
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.utbot.framework.plugin.api.Signature

fun MemberInfo.signature(): Signature =
        (this.member as PsiMethod).signature()

private fun PsiMethod.signature() =
    Signature(this.name, this.parameterList.parameters.map {
        it.type.canonicalText
            .replace("...", "[]") //for PsiEllipsisType
            .replace(",", ", ") // to fix cases like Pair<String,String> -> Pair<String, String>
    })