package org.utbot.cli.language.python

class TestWriter {
    private val testCode: MutableList<String> = mutableListOf()

    fun addTestCode(code: String) {
        testCode.add(code)
    }

    fun generateTestCode(): String {
        val (importLines, code) = testCode.fold(mutableListOf<String>() to StringBuilder()) { acc, s ->
            val lines = s.split(System.lineSeparator())
            val firstClassIndex = lines.indexOfFirst { it.startsWith("class") }
            lines.take(firstClassIndex).forEach { line -> if (line !in acc.first) acc.first.add(line) }
            lines.drop(firstClassIndex).forEach { line -> acc.second.append(line + System.lineSeparator()) }
            acc.first to acc.second
        }
        val codeBuilder = StringBuilder()
        importLines.filter { it.isNotEmpty() }.forEach {
            codeBuilder.append(it)
            codeBuilder.append(System.lineSeparator())
        }
        codeBuilder.append(System.lineSeparator())
        codeBuilder.append(code)
        return codeBuilder.toString()
    }
}