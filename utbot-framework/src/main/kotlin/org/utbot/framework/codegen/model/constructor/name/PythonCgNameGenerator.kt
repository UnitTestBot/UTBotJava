package org.utbot.framework.codegen.model.constructor.name

import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.python.util.toSnakeCase

internal class PythonCgNameGenerator(context_: CgContext): CgNameGeneratorImpl(context_) {

    override fun variableName(type: ClassId, base: String?, isMock: Boolean): String {
        val baseName = base?.toSnakeCase() ?: nameFrom(type)
        return variableName(baseName.toSnakeCase().decapitalize(), isMock)
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
