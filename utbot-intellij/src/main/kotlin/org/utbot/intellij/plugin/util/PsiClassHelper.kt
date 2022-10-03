package org.utbot.intellij.plugin.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifier
import com.intellij.psi.SyntheticElement
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.isGetter
import org.jetbrains.kotlin.asJava.elements.isSetter
import org.utbot.common.filterWhen
import org.utbot.framework.UtSettings

private val PsiMember.isAbstract: Boolean
    get() = modifierList?.hasModifierProperty(PsiModifier.ABSTRACT)?: false


private val PsiMember.isKotlinGetterOrSetter: Boolean
    get() {
        if (this !is KtLightMethod)
            return false
        return isGetter || isSetter
    }

private fun Iterable<MemberInfo>.filterTestableMethods(): List<MemberInfo> = this
    .filterWhen(UtSettings.skipTestGenerationForSyntheticMethods) { it.member !is SyntheticElement }
    .filterNot { it.member.isAbstract }
    .filterNot { it.member.isKotlinGetterOrSetter }

private val PsiClass.isPrivateOrProtected: Boolean
    get() = this.modifierList?.let {
        hasModifierProperty(PsiModifier.PRIVATE) || hasModifierProperty(PsiModifier.PROTECTED)
    } ?: false


// TODO: maybe we need to delete [includeInherited] param here (always false when calling)?
fun PsiClass.extractClassMethodsIncludingNested(includeInherited: Boolean): List<MemberInfo> {
    val ourMethods = TestIntegrationUtils.extractClassMethods(this, includeInherited)
        .filterTestableMethods()

    val methodsFromNestedClasses =
        innerClasses
            .filter { !it.isPrivateOrProtected }
            .flatMap { it.extractClassMethodsIncludingNested(includeInherited) }

    return ourMethods + methodsFromNestedClasses
}

fun PsiClass.extractFirstLevelMembers(includeInherited: Boolean): List<MemberInfo> {
    val methods = TestIntegrationUtils.extractClassMethods(this, includeInherited)
        .filterTestableMethods()
    val classes = if (includeInherited)
        allInnerClasses
    else
        innerClasses
    return methods + classes.filter { !it.isPrivateOrProtected }.map { MemberInfo(it) }
}

val PsiClass.isVisible: Boolean
    get() = generateSequence(this) { it.containingClass }.none { it.isPrivateOrProtected }