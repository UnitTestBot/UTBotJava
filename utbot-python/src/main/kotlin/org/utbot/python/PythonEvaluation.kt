package org.utbot.python

import org.utbot.framework.plugin.api.UtModel
import java.io.File
import java.util.concurrent.TimeUnit

//import org.graalvm.polyglot.Context

object PythonEvaluation {
    fun evaluate(method: PythonMethod, methodArguments: List<UtModel>): String {
//        Thread.currentThread().contextClassLoader = Context::class.java.classLoader
//        val context = Context.newBuilder().allowIO(true).build()
        val arguments = methodArguments.joinToString(transform = { it.toString() })

        val outputFilename = "output_${method.name}.txt"
        val codeFilename = "test_${method.name}.py"

        val methodWithArgs =
            method.asString() +
            "\n" +
            "with open(${outputFilename}, 'w') as fout: print(${method.name}($arguments), file=fout)"

        val file = File(codeFilename)
        file.writeText(methodWithArgs)

        val isCreated = file.createNewFile()
        if (isCreated) {
            "python $codeFilename".runCommand(File("."))
        }

//        val result = context.eval("python", methodWithArgs)
        val resultFile = File(outputFilename)
        file.delete()

        return resultFile.readText()
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