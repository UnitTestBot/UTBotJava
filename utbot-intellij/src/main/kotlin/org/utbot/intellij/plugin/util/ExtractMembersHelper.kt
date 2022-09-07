package org.utbot.intellij.plugin.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.SyntheticElement
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import org.utbot.common.filterWhen
import org.utbot.framework.UtSettings

private val MemberInfoBase<out PsiModifierListOwner>.isAbstract: Boolean
    get() = this.member.modifierList?.hasModifierProperty(PsiModifier.ABSTRACT)?: false

private fun Iterable<MemberInfo>.filterTestableMethods(): List<MemberInfo> = this
    .filterWhen(UtSettings.skipTestGenerationForSyntheticMethods) { it.member !is SyntheticElement }
    .filterNot { it.isAbstract }

// TODO: maybe we need to delete [includeInherited] param here (always false when calling)?
fun PsiClass.extractClassMethodsIncludingNested(includeInherited: Boolean): List<MemberInfo> =
    TestIntegrationUtils.extractClassMethods(this, includeInherited)
        .filterTestableMethods() + innerClasses.flatMap { it.extractClassMethodsIncludingNested(includeInherited) }

fun PsiClass.extractFirstLevelMembers(includeInherited: Boolean): List<MemberInfo> {
    val methods = TestIntegrationUtils.extractClassMethods(this, includeInherited)
        .filterTestableMethods()
    val classes = if (includeInherited)
        allInnerClasses
    else
        innerClasses
    return methods + classes.map { MemberInfo(it) }
}