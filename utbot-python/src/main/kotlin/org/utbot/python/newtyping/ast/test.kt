package org.utbot.python.newtyping.ast

import org.parsers.python.PythonParser
import org.parsers.python.ast.Block
import org.parsers.python.ast.FunctionDefinition
import org.utbot.python.PythonArgument
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.runmypy.getErrorNumber
import org.utbot.python.newtyping.runmypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.runmypy.setConfigFile
import org.utbot.python.utils.Cleaner
import org.utbot.python.utils.TemporaryFileManager

fun main() {
    /*
    TemporaryFileManager.setup()
    val path = "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples/general.py"
    val configFile = setConfigFile(setOf("/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples"))
    val (mypyStorage, report) = readMypyAnnotationStorageAndInitialErrors(
        "python3",
        path,
        configFile
    )
    val storage = PythonTypeStorage.get(mypyStorage)
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
    val method = PythonMethod(
        "get_data_labels",
        null,
        listOf(PythonArgument("dates", null)),
        path,
        null,
        """
        if not dates:
            dates.append(datetime.time(hour=23, minute=59))
            return None
        if all(x.hour == 0 and x.minute == 0 for x in dates):
            return [x.strftime('%Y-%m-%d') for x in dates]
        else:
            return [x.strftime('%H:%M') for x in dates]
        """.trimIndent()
    )
    val visitor = Visitor(listOf(collector))
    visitor.visit(functionBlock)
    println("Started inference")
    BaselineAlgorithm(
        storage,
        "python3",
        method,
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples"),
        moduleToImport = "general",
        getErrorNumber(report, path, 165, 171)
    ).run(collector.result).take(100).forEach {
        println(it.pythonTypeRepresentation())
    }

    Cleaner.doCleaning()
     */
}