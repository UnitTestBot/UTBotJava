package org.utbot.python.framework.codegen.model.constructor.name

import org.utbot.python.framework.codegen.model.PythonImport
import org.utbot.framework.codegen.services.language.isLanguageKeyword
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.services.CgNameGenerator
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.util.toSnakeCase

internal fun infiniteInts(): Sequence<Int> =
    generateSequence(1) { it + 1 }

class PythonCgNameGenerator(val context: CgContext)
        : CgNameGenerator, CgContextOwner by context {

    private fun nextIndexedVarName(base: String): String =
        infiniteInts()
            .map { "$base$it" }
            .first { it !in existingVariableNames }

    private fun nextIndexedMethodName(base: String, skipOne: Boolean = false): String =
        infiniteInts()
            .map { if (skipOne && it == 1) base else "$base$it" }
            .first { it !in existingMethodNames }

    private fun createNameFromKeyword(baseName: String): String =
        nextIndexedVarName(baseName)

    private fun createExecutableName(executableId: ExecutableId): String {
        return when (executableId) {
            is ConstructorId -> executableId.classId.prettifiedName
            is MethodId -> executableId.name
        }
    }

    override fun nameFrom(id: ClassId): String =
        when (id) {
            is NormalizedPythonAnnotation -> "var"
            else -> id.simpleName.toSnakeCase()
        }

    override fun variableName(base: String, isMock: Boolean, isSpy: Boolean, isStatic: Boolean): String {
        val baseName = when {
            isMock -> base + "_mock"
            isSpy -> base + "_spy"
            isStatic -> base + "_static"
            else -> base
        }
        return when {
            baseName in existingVariableNames -> nextIndexedVarName(baseName)
            isLanguageKeyword(baseName, context.cgLanguageAssistant) -> createNameFromKeyword(baseName)
            else -> baseName
        }.also {
            existingVariableNames = existingVariableNames.add(it)
        }
    }

    override fun variableName(type: ClassId, base: String?, isMock: Boolean, isSpy: Boolean): String {
        val baseName = base?.toSnakeCase() ?: nameFrom(type)
        val importedModuleNames = collectedImports.mapNotNull {
            if (it is PythonImport) it.rootModuleName else null
        }
        return when {
            baseName in existingVariableNames -> nextIndexedVarName(baseName)
            baseName in importedModuleNames -> nextIndexedVarName(baseName)
            isLanguageKeyword(baseName, context.cgLanguageAssistant) -> createNameFromKeyword(baseName)
            else -> baseName
        }.also {
            existingVariableNames = existingVariableNames.add(it)
        }
    }

    override fun testMethodNameFor(executableId: ExecutableId, customName: String?): String {
        val executableName = createExecutableName(executableId)

        val name = if (customName != null && customName !in existingMethodNames) {
            customName
        } else {
            val base = customName ?: "test_${executableName.toSnakeCase()}"
            nextIndexedMethodName(base)
        }
        existingMethodNames += name
        return name
    }

    override fun parameterizedTestMethodName(dataProviderMethodName: String): String {
        TODO("Not yet implemented")
    }

    override fun dataProviderMethodNameFor(executableId: ExecutableId): String {
        TODO("Not yet implemented")
    }

    override fun errorMethodNameFor(executableId: ExecutableId): String {
        val executableName = createExecutableName(executableId)
        val newName = when (val base = "test_${executableName.toSnakeCase()}_errors") {
            !in existingMethodNames -> base
            else -> nextIndexedMethodName(base)
        }
        existingMethodNames += newName
        return newName
    }
}
