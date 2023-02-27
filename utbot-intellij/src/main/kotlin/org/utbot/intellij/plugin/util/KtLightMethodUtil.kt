package org.utbot.intellij.plugin.util

import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty


/*
 * TODO: Remove the following methods after Kotlin version transition from [1.7.20].
 * See [https://github.com/UnitTestBot/UTBotJava/issues/1793].
 */
val KtLightMethod.isGetter: Boolean
    get() = isAccessor(true)

val KtLightMethod.isSetter: Boolean
    get() = isAccessor(false)

private fun KtLightMethod.isAccessor(getter: Boolean): Boolean {
    val origin = kotlinOrigin as? KtCallableDeclaration ?: return false
    if (origin !is KtProperty && origin !is KtParameter) return false
    val expectedParametersCount = (if (getter) 0 else 1) + (if (origin.receiverTypeReference != null) 1 else 0)
    return parameterList.parametersCount == expectedParametersCount
}