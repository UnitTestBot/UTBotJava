package org.utbot.intellij.plugin.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.SyntheticElement
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.isGetter
import org.jetbrains.kotlin.asJava.elements.isSetter
import org.utbot.common.filterWhen
import org.utbot.framework.UtSettings

private val MemberInfoBase<out PsiModifierListOwner>.isAbstract: Boolean
    get() = this.member.modifierList?.hasModifierProperty(PsiModifier.ABSTRACT)?: false


private val MemberInfo.isKotlinGetterOrSetter: Boolean
    get() {
        if (this !is KtLightMethod)
            return false
        return this.isGetter || this.isSetter
    }

private fun Iterable<MemberInfo>.filterTestableMethods(): List<MemberInfo> = this
    .filterWhen(UtSettings.skipTestGenerationForSyntheticMethods) { it.member !is SyntheticElement }
    .filterNot { it.isAbstract }
    .filterNot { it.isKotlinGetterOrSetter }

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