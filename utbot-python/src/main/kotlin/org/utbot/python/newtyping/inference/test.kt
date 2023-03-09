package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/Python"),
        "/home/tochilinak/Documents/projects/utbot/Python/machine_learning/scoring_functions.py",
        moduleOfSourceFile = "machine_learning.scoring_functions",
        "mae",
        className = null
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}