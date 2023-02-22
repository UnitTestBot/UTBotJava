package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3.9",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples"),
        "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples/boruvka.py",
        moduleOfSourceFile = "easy_samples.boruvka",
        "boruvka",
        className = "Graph"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}