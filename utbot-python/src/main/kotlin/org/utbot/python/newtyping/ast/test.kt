package org.utbot.python.newtyping.ast

import org.parsers.python.PythonParser
import org.parsers.python.ast.Block
import org.parsers.python.ast.FunctionDefinition
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm

fun main() {
    val mypyStorage = HintCollector::class.java.getResource("/annotation_sample.json")?.let { readMypyAnnotationStorage(it.readText()) }
    val storage = PythonTypeStorage.get(mypyStorage!!)

    val content = """
        import collections

        def f(x, i):
            res = x[i:i+2:-1][0]
            res += 1
            y = res = 1 + 2
            y = [1, 2, 3, len(x), 1j, None, "123"]
            z = []
            w = [1]
            w = [len(x)]
            # x, y = res
            if i > 0 and True or (not True):
                return 1
            elif len(x) == 0:
                return 2
            else:
                for elem in x:
                    res += elem
            return res
    """.trimIndent()
    val root = PythonParser(content).Module()
    val functionBlock = root.children().first { it is FunctionDefinition }.children().first { it is Block }
    val collector = HintCollector(
        createPythonCallableType(
            0,
            listOf(PythonCallableTypeDescription.ArgKind.Positional, PythonCallableTypeDescription.ArgKind.Positional),
            listOf("x", "i"),
            isClassMethod = false,
            isStaticMethod = false
        ) {
            FunctionTypeCreator.InitializationData(listOf(pythonAnyType, pythonAnyType), pythonAnyType)
        },
        storage
    )
    val visitor = Visitor(listOf(collector))
    visitor.visit(functionBlock)
    BaselineAlgorithm(storage).run(collector.result).forEach {
        println(it.pythonTypeRepresentation())
    }
}