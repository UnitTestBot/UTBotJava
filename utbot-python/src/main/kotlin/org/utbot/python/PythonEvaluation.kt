package org.utbot.python

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.stringClassId
import java.io.File
import java.util.concurrent.TimeUnit

//import org.graalvm.polyglot.Context

object PythonEvaluation {
    fun evaluate(method: PythonMethod, methodArguments: List<UtModel>, testSourceRoot: String): String {
//        Thread.currentThread().contextClassLoader = Context::class.java.classLoader
//        val context = Context.newBuilder().allowIO(true).build()
        createDirectory(testSourceRoot)

        val arguments = methodArguments.joinToString(transform = { it.toString() })

        val outputFilename = "$testSourceRoot/output_utbot_run_${method.name}.txt"
        val codeFilename = "$testSourceRoot/test_utbot_run_${method.name}.py"

        val methodWithArgs =
            method.asString() +
            "\n" +
            "with open('$outputFilename', 'w') as fout: print(${method.name}($arguments).__repr__(), file=fout, end='')"

        val file = File(codeFilename)
        file.writeText(methodWithArgs)
        file.createNewFile()

        val process = Runtime.getRuntime().exec("python3 $codeFilename")
        process.waitFor()

//        val result = context.eval("python", methodWithArgs)

        val resultFile = File(outputFilename)

        val output = resultFile.readText()
        resultFile.delete()
        file.delete()
        return output
    }

    private fun createDirectory(path: String) {
        File(path).mkdir()
    }
}