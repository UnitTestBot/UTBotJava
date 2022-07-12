package org.utbot.python

import org.utbot.framework.plugin.api.UtModel
import java.io.File
import java.util.concurrent.TimeUnit

//import org.graalvm.polyglot.Context

object PythonEvaluation {
    fun evaluate(method: PythonMethod, methodArguments: List<UtModel>, testSourceRoot: String): String {
//        Thread.currentThread().contextClassLoader = Context::class.java.classLoader
//        val context = Context.newBuilder().allowIO(true).build()
        createDirectory(testSourceRoot)

        val arguments = methodArguments.joinToString(transform = { it.toString() })

        val outputFilename = "$testSourceRoot/output_${method.name}.txt"
        val codeFilename = "$testSourceRoot/test_${method.name}.py"

        val methodWithArgs =
            method.asString() +
            "\n" +
            "with open('$outputFilename', 'w') as fout: print(${method.name}($arguments), file=fout, end='')"

        val file = File(codeFilename)
        file.writeText(methodWithArgs)

        file.createNewFile()
        Runtime.getRuntime().exec("python3 $codeFilename")

//        val result = context.eval("python", methodWithArgs)
        val resultFile = File(outputFilename)
        file.delete()

        val output = resultFile.readText()
        resultFile.delete()
        return output
    }

    private fun createDirectory(path: String) {
        File(path).mkdir()
    }
}

fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(60, TimeUnit.MINUTES)
}