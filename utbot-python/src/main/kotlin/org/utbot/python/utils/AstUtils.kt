package org.utbot.python.utils

import org.parsers.python.ast.ClassDefinition
import org.parsers.python.ast.FunctionDefinition
import org.parsers.python.ast.Module
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.ast.ParsedFunctionDefinition
import org.utbot.python.newtyping.ast.parseFunctionDefinition

object AstUtils {
    fun getTopLevelFunctions(parsedFile: Module): List<FunctionDefinition> {
        return parsedFile.children().filterIsInstance<FunctionDefinition>()
    }

    fun getTopLevelClasses(parsedFile: Module): List<ClassDefinition> {
        return parsedFile.children().filterIsInstance<ClassDefinition>()
    }

    fun getClassMethods(class_: ClassDefinition): List<FunctionDefinition> {
        return class_.children().filterIsInstance<FunctionDefinition>()
    }

    fun findFunctionDefinition(parsedFile: Module, method: PythonMethod): ParsedFunctionDefinition {
        return if (method.containingPythonClassId == null) {
            getTopLevelFunctions(parsedFile).mapNotNull {
                parseFunctionDefinition(it)
            }.firstOrNull {
                it.name.toString() == method.name
            } ?: throw Exception("Couldn't find top-level function ${method.name}")
        } else {
            getTopLevelClasses(parsedFile)
                .map { getClassMethods(it) }
                .flatten()
                .mapNotNull { parseFunctionDefinition(it) }
                .firstOrNull {
                    it.name.toString() == method.name
                } ?: throw Exception("Couldn't find top-level function ${method.name}")
        }
    }
}