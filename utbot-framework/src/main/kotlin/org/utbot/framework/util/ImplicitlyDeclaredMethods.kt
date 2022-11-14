package org.utbot.framework.util

import org.utbot.framework.codegen.model.util.fieldThatIsGotWith
import org.utbot.framework.codegen.model.util.fieldThatIsSetWith
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.util.isData
import org.utbot.framework.plugin.api.util.isEnum
import org.utbot.framework.plugin.api.util.isFromKotlin
import org.utbot.framework.plugin.api.util.isKotlinFile

/**
 * Returns whether this method could be implicitly generated by compiler, or not.
 *
 * Note that here we can only judge this by method name and kind of class (data class, enum, etc).
 * There seems to be no (at least, easy) way to check from bytecode if this method was actually overridden by user,
 * so this function will return true even if the matching method is not autogenerated but written explicitly by user.
 */
val ExecutableId.isKnownImplicitlyDeclaredMethod: Boolean
    get() =
        when {
            // this check is needed because reflection is not fully supported for file facades -- see KT-16479,
            // by now we assume that such classes can't contain autogenerated methods
            classId.isKotlinFile -> false
            isKotlinGetterOrSetter -> true
            classId.isEnum -> name in KnownImplicitlyDeclaredMethods.enumImplicitlyDeclaredMethodNames
            classId.isData -> KnownImplicitlyDeclaredMethods.dataClassImplicitlyDeclaredMethodNameRegexps.any { it.matches(name) }
            else -> false
        }

internal val ExecutableId.isKotlinGetterOrSetter: Boolean
    get() = classId.isFromKotlin &&
            (classId.fieldThatIsGotWith(this) != null || classId.fieldThatIsSetWith(this) != null)

/**
 * Contains names of methods that are always autogenerated by compiler and thus it is unlikely that
 * one would want to generate tests for them.
 */
private object KnownImplicitlyDeclaredMethods {
    /** List with names of enum methods that are generated by compiler */
    val enumImplicitlyDeclaredMethodNames = listOf("values", "valueOf")

    /** List with regexps that match names of methods that are generated by Kotlin compiler for data classes */
    val dataClassImplicitlyDeclaredMethodNameRegexps = listOf(
        "equals",
        "hashCode",
        "toString",
        "copy",
        "component[1-9][0-9]*"
    ).map { it.toRegex() }
}