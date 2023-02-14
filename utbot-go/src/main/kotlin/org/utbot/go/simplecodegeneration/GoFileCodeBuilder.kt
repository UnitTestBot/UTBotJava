package org.utbot.go.simplecodegeneration

import org.utbot.go.framework.api.go.GoImport
import org.utbot.go.framework.api.go.GoPackage

class GoFileCodeBuilder(
    sourcePackage: GoPackage,
    imports: Set<GoImport>,
) {
    private val packageLine: String = "package ${sourcePackage.packageName}"
    private val importLines: String = importLines(imports)
    private val topLevelElements: MutableList<String> = mutableListOf()

    private fun importLines(imports: Set<GoImport>): String {
        if (imports.isEmpty()) return ""
        if (imports.size == 1) {
            return "import ${imports.first()}"
        }

        return imports.sortedBy { it.toString() }.joinToString(separator = "", prefix = "import (\n", postfix = ")") {
            "\t$it\n"
        }
    }

    fun buildCodeString(): String {
        return "$packageLine\n\n$importLines\n\n${topLevelElements.joinToString(separator = "\n\n")}"
    }

    fun addTopLevelElements(vararg elements: String) {
        topLevelElements.addAll(elements)
    }

    fun addTopLevelElements(elements: Iterable<String>) {
        topLevelElements.addAll(elements)
    }
}