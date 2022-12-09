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

        def f(x, i, j):
            y = [1, 2, 3]
            x += y
            i = 0
            if j.hour > 0:
                return None
    """.trimIndent()

    val content1 = """
        import datetime


        def get_data_labels(dates):
            if not dates:
                dates.append(datetime.time(hour=23, minute=59))
                return None
            if all(x.hour == 0 and x.minute == 0 for x in dates):
                return [x.strftime('%Y-%m-%d') for x in dates]
            else:
                return [x.strftime('%H:%M') for x in dates]
    """.trimIndent()

    val root = PythonParser(content1).Module()
    val functionBlock = root.children().first { it is FunctionDefinition }.children().first { it is Block }
    val collector = HintCollector(
        createPythonCallableType(
            0,
            //List(3) { PythonCallableTypeDescription.ArgKind.Positional },
            listOf(PythonCallableTypeDescription.ArgKind.Positional),
            listOf("dates"),
            //listOf("x", "i", "j"),
            isClassMethod = false,
            isStaticMethod = false
        ) {
            FunctionTypeCreator.InitializationData(listOf(pythonAnyType), pythonAnyType)
            //FunctionTypeCreator.InitializationData(List(3) { pythonAnyType }, pythonAnyType)
        },
        storage
    )
    val visitor = Visitor(listOf(collector))
    visitor.visit(functionBlock)
    BaselineAlgorithm(storage).run(collector.result).take(100).forEach {
        println(it.pythonTypeRepresentation())
    }
}