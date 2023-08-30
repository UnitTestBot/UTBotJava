package org.utbot.python.newtyping.samples

import org.utbot.python.newtyping.mypy.MypyBuildDirectory
import org.utbot.python.newtyping.mypy.buildMypyInfo
import java.io.File

fun main(args: Array<String>) {
    val pythonPath = args[0]
    val source = File(args[1])
    val module = args[2]
    val buildDir = File(args[3])
    val jsonName = args[4]
    val path = source.parent
    val mypyDir = MypyBuildDirectory(buildDir, setOf(path))
    buildMypyInfo(pythonPath, listOf(source.canonicalPath), listOf(module), mypyDir, module)
    val jsonText = mypyDir.fileForAnnotationStorage.readText()
    val output = File(jsonName)
    output.writeText(jsonText)
    output.createNewFile()
}