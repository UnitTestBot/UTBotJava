package org.utbot.python.newtyping.ast

import org.parsers.python.PythonParser
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.hints.FunctionParameter
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.pythonAnyType
import org.utbot.python.newtyping.readMypyAnnotationStorage

fun main() {
    HintCollector::class.java.getResource("/annotation_sample.json")?.let { readMypyAnnotationStorage(it.readText()) }

    val content = """
        import collections

        def f(x, i):
            res = x[i:i+2:-1][0]
            if i > 0 and True or (not True):
                return 1
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