package org.utbot.framework.codegen.model.constructor.name

import org.utbot.framework.codegen.PythonImport
import org.utbot.framework.codegen.isLanguageKeyword
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.python.util.toSnakeCase

internal class PythonCgNameGenerator(context_: CgContext): CgNameGeneratorImpl(context_) {
    override fun nameFrom(id: ClassId): String =
        when (id) {
            is NormalizedPythonAnnotation -> "var"
            else -> id.simpleName.toSnakeCase()
        }

    override fun variableName(type: ClassId, base: String?, isMock: Boolean): String {
        val baseName = base?.toSnakeCase() ?: nameFrom(type)
        val importedModuleNames = collectedImports.mapNotNull {
            if (it is PythonImport) it.rootModuleName else null
        }
        return when {
            baseName in existingVariableNames -> nextIndexedVarName(baseName)
            baseName in importedModuleNames -> nextIndexedVarName(baseName)
            isLanguageKeyword(baseName, codegenLanguage) -> createNameFromKeyword(baseName)
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
