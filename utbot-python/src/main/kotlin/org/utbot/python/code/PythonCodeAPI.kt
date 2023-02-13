package org.utbot.python.code

import org.parsers.python.ast.Block
import org.parsers.python.ast.ClassDefinition
import org.parsers.python.ast.Module
import org.parsers.python.ast.FunctionDefinition
import org.utbot.python.PythonMethodHeader
import org.utbot.python.newtyping.ast.ParsedFunctionDefinition
import org.utbot.python.newtyping.ast.parseClassDefinition
import org.utbot.python.newtyping.ast.parseFunctionDefinition

object PythonCode {

    fun getTopLevelFunctions(parsedFile: Module): List<FunctionDefinition> {
        return parsedFile.children().filterIsInstance<FunctionDefinition>()
    }

    fun getTopLevelClasses(parsedFile: Module): List<ClassDefinition> {
        return parsedFile.children().filterIsInstance<ClassDefinition>()
    }

    fun getClassMethods(class_: Block): List<FunctionDefinition> {
        return class_.children().filterIsInstance<FunctionDefinition>()
    }

    fun findFunctionDefinition(parsedFile: Module, method: PythonMethodHeader): ParsedFunctionDefinition {
        return if (method.containingPythonClassId == null) {
            getTopLevelFunctions(parsedFile).mapNotNull {
                parseFunctionDefinition(it)
            }.firstOrNull {
                it.name.toString() == method.name
            } ?: throw Exception("Couldn't find top-level function ${method.name}")
        } else {
            getTopLevelClasses(parsedFile)
                .mapNotNull { parseClassDefinition(it) }
                .flatMap { getClassMethods(it.body) }
                .mapNotNull { parseFunctionDefinition(it) }
                .firstOrNull {
                    it.name.toString() == method.name
                } ?: throw Exception("Couldn't find method ${method.name}")
        }
    }
}
