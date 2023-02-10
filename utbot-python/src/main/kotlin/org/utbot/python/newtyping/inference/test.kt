package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/samples"),
        "/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/samples/type_inference.py",
        moduleOfSourceFile = "type_inference",
        "type_inference"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}