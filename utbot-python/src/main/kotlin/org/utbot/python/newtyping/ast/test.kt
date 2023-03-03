package org.utbot.python.newtyping.ast

import org.parsers.python.PythonParser
import org.utbot.python.PythonMethodHeader
import org.utbot.python.code.PythonCode

fun main() {
    val content = """
    class A:
        @decorator
        def func(x):
            return 1
    """.trimIndent()

    val root = PythonParser(content).Module()
    // val y = PythonCode.findFunctionDefinition(root, PythonMethodHeader("func", "", null))
    val x = root
}