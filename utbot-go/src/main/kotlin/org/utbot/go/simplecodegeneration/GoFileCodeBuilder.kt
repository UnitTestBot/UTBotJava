package org.utbot.go.simplecodegeneration

class GoFileCodeBuilder(
    packageName: String,
    importNames: Set<String>,
) {
    private val packageLine: String = "package $packageName"
    private val importLines: String = importLines(importNames)
    private val topLevelElements: MutableList<String> = mutableListOf()

    private fun importLines(importNames: Set<String>): String {
        val sortedImportNames = importNames.toList().sorted()
        if (sortedImportNames.isEmpty()) return ""
        if (sortedImportNames.size == 1) {
            return "import ${sortedImportNames.first()}"
        }
        return sortedImportNames.joinToString(separator = "", prefix = "import (\n", postfix = ")") {
            "\t\"$it\"\n"
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