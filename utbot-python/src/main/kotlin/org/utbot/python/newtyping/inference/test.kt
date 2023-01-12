package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples"),
        "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples/general.py",
        moduleOfSourceFile = "general",
        "fact"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}