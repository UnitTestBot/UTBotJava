package org.utbot.python.newtyping.mypy

import java.io.File

fun main() {
    val started = System.currentTimeMillis()
    val filename = "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python-types/src/main/python/utbot_mypy_runner/out.json"
    val text = File(filename).readText()
    println("Read text in ${System.currentTimeMillis() - started} ms")
    readMypyInfoBuildWithoutRoot(text)
    println("Finished in ${System.currentTimeMillis() - started} ms")
}