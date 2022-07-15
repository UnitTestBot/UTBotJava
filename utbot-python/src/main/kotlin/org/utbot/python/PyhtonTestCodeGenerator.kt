package org.utbot.python

import org.utbot.framework.plugin.api.UtExecution
import java.io.File

object PyhtonTestCodeGenerator {
    fun generateTestCode(testCase: PythonTestCase): List<String> {
        return testCase.executions.mapIndexed { index, utExecution ->
            generateTestCode(testCase.method, utExecution, index)
        }
    }

    fun generateTestCode(method: PythonMethod, execution: UtExecution, number: Int): String {
        val testFunctionName = "execution.testMethodName_$number"
        val testFunctionTitle = "def $testFunctionName():"
        val arguments = execution.stateBefore.parameters.zip(method.arguments).map { (model, argument) ->
            "${argument.name} = $model"
        }
        val functionArguments = method.arguments.map { argument ->
            "${argument.name}=${argument.name}"
        }
        val actualName = "actual"
        val functionCall = listOf("$actualName = ${method.name}(") +
                addIndent(functionArguments.map {
                    "$it,"
                }) +
                listOf(")")

        val correctResultName = "correct_result"
        val correctResult = "$correctResultName = ${execution.result}"
        val assertLine = "assert $actualName == $correctResultName"

        val codeRows = arguments + functionCall + listOf(correctResult, assertLine)
        val functionRows = listOf(testFunctionTitle) + addIndent(codeRows)
        return functionRows.joinToString("\n")
    }

    private fun addIndent(rows: List<String>, indentLevel: Int = 1): List<String> {
        return rows.map {
            addIndent(it, indentLevel)
        }
    }
    private fun addIndent(row: String, indentLevel: Int = 1): String {
        val indent = " ".repeat(4)
        return indent.repeat(indentLevel) + row
    }

    fun saveToFile(filePath: String, code: List<String>) {
        val file = File(filePath)
        file.writeText(code.joinToString("\n\n\n"))
        file.createNewFile()
    }
}