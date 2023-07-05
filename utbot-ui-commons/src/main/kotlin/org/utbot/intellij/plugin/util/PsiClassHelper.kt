package org.utbot.intellij.plugin.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.SyntheticElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.project.Project
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.isGetter
import org.jetbrains.kotlin.asJava.elements.isSetter
import org.jetbrains.kotlin.psi.KtClass
import org.utbot.common.filterWhen
import org.utbot.framework.UtSettings
import org.utbot.intellij.plugin.models.packageName

/**
 * Used to build binary name from canonical name
 * in a similar form which could be obtained by [java.lang.Class.getName] method.
 *
 * E.g. ```org.example.OuterClass.InnerClass.InnerInnerClass``` -> ```org.example.OuterClass$InnerClass$InnerInnerClass```
 */
val PsiClass.binaryName: String
    get() =
        if (packageName.isEmpty()) {
            qualifiedName?.replace(".", "$") ?: ""
        } else {
            val name =
                qualifiedName
                    ?.substringAfter("$packageName.")
                    ?.replace(".", "$")
                    ?: error("Binary name construction failed: unable to get qualified name of $this")
            "$packageName.$name"
        }

val PsiMember.isAbstract: Boolean
    get() = modifierList?.hasModifierProperty(PsiModifier.ABSTRACT)?: false

val PsiMember.isStatic: Boolean
    get() = modifierList?.hasModifierProperty(PsiModifier.STATIC)?: false

private val PsiMember.isKotlinGetterOrSetter: Boolean
    get() {
        if (this !is KtLightMethod)
            return false
        return this.isGetter || this.isSetter
    }

private val PsiMember.isKotlinAndProtected: Boolean
    get() = this is KtLightMember<*> && this.hasModifierProperty(PsiModifier.PROTECTED)

// By now, we think that method in Kotlin is autogenerated iff navigation to its declaration leads to its declaring class
// rather than the method itself (because such methods don't have bodies that we can navigate to)
private val PsiMember.isKotlinAutogeneratedMethod: Boolean
    get() = this is KtLightMethod && navigationElement is KtClass

private val PsiMethod.canBeCalledStatically: Boolean
    get() = isStatic || containingClass?.let { it.isStatic && !it.isInterface && !it.isAbstract } ?: throw IllegalStateException("No containing class found for method $this")

private val PsiMethod.isUntestableMethodOfAbstractOrInterface: Boolean
    get() {
        val hasAbstractContext = generateSequence(containingClass) { it.containingClass }.any { it.isAbstract || it.isInterface }
        return hasAbstractContext && !canBeCalledStatically
    }

private fun Iterable<MemberInfo>.filterTestableMethods(): List<MemberInfo> = this
    .filterWhen(UtSettings.skipTestGenerationForSyntheticAndImplicitlyDeclaredMethods) {
        it.member !is SyntheticElement && !it.member.isKotlinAutogeneratedMethod
    }
    .filterNot { (it.member as PsiMethod).isUntestableMethodOfAbstractOrInterface }
    .filterNot { it.member.isKotlinGetterOrSetter }
    .filterNot { it.member.isKotlinAndProtected }

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

object PsiClassHelper {
    /**
     * Finds [PsiClass].
     *
     * @param name binary name which is converted to canonical name.
     */
    fun findClass(name: String, project: Project): PsiClass? {
        // Converting name to canonical name
        val canonicalName = name.replace("$", ".")
        return JavaPsiFacade
            .getInstance(project)
            .findClass(canonicalName, GlobalSearchScope.projectScope(project))
    }
}