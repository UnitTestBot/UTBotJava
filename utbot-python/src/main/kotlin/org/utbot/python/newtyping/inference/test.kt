package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3.9",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples"),
        "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/easy_samples/generics.py",
        moduleOfSourceFile = "easy_samples.generics",
        "set",
        className = "LoggedVar"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}