package org.utbot.framework.codegen.model.constructor.name

import org.utbot.framework.codegen.isLanguageKeyword
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.infiniteInts
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.isArray

/**
 * Interface for method and variable name generators
 */
internal interface CgNameGenerator {
    /**
     * Generate a variable name given a [base] name.
     * @param isMock denotes whether a variable represents a mock object or not
     * @param isStatic denotes whether a variable represents a static variable or not
     */
    fun variableName(base: String, isMock: Boolean = false, isStatic: Boolean = false): String

    /**
     * Convert a given class id to a string that can serve
     * as a part of a variable name
     */
    fun nameFrom(id: ClassId): String =
            when {
                id.isAnonymous -> id.prettifiedName
                id.isArray -> id.prettifiedName
                id.simpleName.isScreamingSnakeCase() -> id.simpleName.fromScreamingSnakeCaseToCamelCase() // special case for enum instances
                else -> id.simpleName.decapitalize()
            }

    /**
     * Generate a variable name given a [type] of variable and a [base] name
     * If [base] is not null, then use it to generate name
     * Otherwise, fall back to generating a name by [type]
     * @param isMock denotes whether a variable represents a mock object or not
     */
    fun variableName(type: ClassId, base: String? = null, isMock: Boolean = false): String

    /**
     * Generate a new test method name.
     */
    fun testMethodNameFor(executableId: ExecutableId, customName: String? = null): String

    /**
     * Generates a new parameterized test method name by data provider method name.
     */
    fun parameterizedTestMethodName(dataProviderMethodName: String): String

    /**
     * Generates a new data for parameterized test provider method name
     */
    fun dataProviderMethodNameFor(executableId: ExecutableId): String

    /**
     * Generate a new error method name
     */
    fun errorMethodNameFor(executableId: ExecutableId): String
}

/**
 * Class that generates names for methods and variables
 * To avoid name collisions it uses existing names information from CgContext
 */
internal class CgNameGeneratorImpl(private val context: CgContext)
    : CgNameGenerator, CgContextOwner by context {

    override fun variableName(base: String, isMock: Boolean, isStatic: Boolean): String {
        val baseName = when {
            isMock -> base + "Mock"
            isStatic -> base + "Static"
            else -> base
        }
        return when {
            baseName in existingVariableNames -> nextIndexedVarName(baseName)
            isLanguageKeyword(baseName, codegenLanguage) -> createNameFromKeyword(baseName)
            else -> baseName
        }.also {
            existingVariableNames = existingVariableNames.add(it)
        }
    }

    override fun variableName(type: ClassId, base: String?, isMock: Boolean): String {
        val baseName = base?.fromScreamingSnakeCaseToCamelCase() ?: nameFrom(type)
        return variableName(baseName.decapitalize(), isMock)
    }

    override fun testMethodNameFor(executableId: ExecutableId, customName: String?): String {
        val executableName = createExecutableName(executableId)

        // no index suffix allowed only when there's a vacant custom name
        val name = if (customName != null && customName !in existingMethodNames) {
            customName
        } else {
            val base = customName ?: "test${executableName.capitalize()}"
            nextIndexedMethodName(base)
        }
        existingMethodNames += name
        return name
    }

    private val dataProviderMethodPrefix = "provideDataFor"

    override fun parameterizedTestMethodName(dataProviderMethodName: String) =
        dataProviderMethodName.replace(dataProviderMethodPrefix, "parameterizedTestsFor")

    override fun dataProviderMethodNameFor(executableId: ExecutableId): String {
        val executableName = createExecutableName(executableId)
        val indexedName = nextIndexedMethodName(executableName.capitalize(), skipOne = true)

        existingMethodNames += indexedName
        return "$dataProviderMethodPrefix$indexedName"
    }

    override fun errorMethodNameFor(executableId: ExecutableId): String {
        val executableName = createExecutableName(executableId)
        val newName = when (val base = "test${executableName.capitalize()}_errors") {
            !in existingMethodNames -> base
            else -> nextIndexedMethodName(base)
        }
        existingMethodNames += newName
        return newName
    }

    /**
     * Creates a new indexed variable name by [base] name.
     */
    private fun nextIndexedVarName(base: String): String =
        infiniteInts()
            .map { "$base$it" }
            .first { it !in existingVariableNames }

    /**
     * Creates a new indexed methodName by [base] name.
     *
     * @param skipOne shows if we add "1" to first method name or not
     */
    private fun nextIndexedMethodName(base: String, skipOne: Boolean = false): String =
        infiniteInts()
            .map { if (skipOne && it == 1) base else "$base$it" }
            .first { it !in existingMethodNames }

    private fun createNameFromKeyword(baseName: String): String = when(codegenLanguage) {
        CodegenLanguage.JAVA -> nextIndexedVarName(baseName)
        CodegenLanguage.KOTLIN -> {
            // use backticks for first variable with keyword name and use indexed names for all next such variables
            if (baseName !in existingVariableNames) "`$baseName`" else nextIndexedVarName(baseName)
        }
    }

    private fun createExecutableName(executableId: ExecutableId): String {
        return when (executableId) {
            is ConstructorId -> executableId.classId.prettifiedName // TODO: maybe we need some suffix e.g. "Ctor"?
            is MethodId -> executableId.name
        }
    }
}

/**
 * Checks names like JUST_COLOR.
 * @see <a href="https://en.wikipedia.org/wiki/Naming_convention_(programming)#Examples_of_multiple-word_identifier_formats">SCREAMING_SNAKE_CASE</a>
 */
private fun String.isScreamingSnakeCase(): Boolean {
    // the code below is a bit complicated, but we want to support non-latin letters too,
    // so we cannot just use such regex: [A-Z0-9]+(_[A-Z0-9]+)*
    return split("_").all {
        word -> word.isNotEmpty() && word.all { c -> c.isUpperCase() || c.isDigit() }
    }
}

/**
 * Transforms string in SCREAMING_SNAKE_CASE to camelCase. If string is not in SCREAMING_SNAKE_CASE, returns it as it is.
 * @see [isScreamingSnakeCase]
 */
private fun String.fromScreamingSnakeCaseToCamelCase(): String {
    if (!isScreamingSnakeCase()) return this

    val lowerCaseWords = split("_").map { it.toLowerCase() }
    val firstWord = lowerCaseWords.first()
    val otherWords = lowerCaseWords.drop(1).map { it.capitalize() }

    val transformedWords = listOf(firstWord) + otherWords

    return transformedWords.joinToString("")
}
