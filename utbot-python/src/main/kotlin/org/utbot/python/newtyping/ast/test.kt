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
    val content = """
    def repr_test(x):
        x *= 100
        return [1, x + 1, collections.UserList([1, 2, 3]), collections.Counter("flkafksdf"), collections.OrderedDict({1: 2, 4: "jflas"})]
    """.trimIndent()

    val root = PythonParser(content).Module()
    val x = root
}