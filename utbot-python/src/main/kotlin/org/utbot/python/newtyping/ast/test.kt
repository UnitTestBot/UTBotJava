package org.utbot.python.newtyping.ast

import org.parsers.python.PythonParser
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.hints.FunctionParameter
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.pythonAnyType


fun main() {
    val content = """
        import collections

        def f(x, i):
            res = x[i:i+2:-1][0]
            for elem in x:
                res += elem
            return res
    """.trimIndent()
    val root = PythonParser(content).Module()
    val collector = HintCollector(
        listOf(FunctionParameter("x", pythonAnyType), FunctionParameter("i", pythonAnyType))
    )
    val visitor = Visitor(listOf(collector))
    visitor.visit(root)
    val x = root.beginLine
}