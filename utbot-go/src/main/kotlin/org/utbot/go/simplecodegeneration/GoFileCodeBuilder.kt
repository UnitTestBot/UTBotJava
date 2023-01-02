package org.utbot.go.simplecodegeneration

class GoFileCodeBuilder {

    private var packageLine: String? = null
    private var importLines: String? = null
    private val topLevelElements: MutableList<String> = mutableListOf()

    fun buildCodeString(): String {
        return "$packageLine\n\n$importLines\n\n${topLevelElements.joinToString(separator = "\n\n")}"
    }

    fun setPackage(packageName: String) {
        packageLine = "package $packageName"
    }

    fun setImports(importNames: Set<String>) {
        val sortedImportNames = importNames.toList().sorted()
        if (sortedImportNames.isEmpty()) return
        if (sortedImportNames.size == 1) {
            importLines = "import ${sortedImportNames.first()}"
            return
        }
        importLines = sortedImportNames.joinToString(separator = "", prefix = "import(\n", postfix = ")") {
            "\t\"$it\"\n"
        }
    }

    fun addTopLevelElements(vararg elements: String) {
        topLevelElements.addAll(elements)
    }

    fun addTopLevelElements(elements: Iterable<String>) {
        topLevelElements.addAll(elements)
    }
}