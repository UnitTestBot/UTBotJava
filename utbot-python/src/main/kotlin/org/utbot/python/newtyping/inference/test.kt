package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/PythonRobotics"),
        "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/DynamicWindowApproach/dynamic_window_approach.py",
        moduleOfSourceFile = "PathPlanning.DynamicWindowApproach.dynamic_window_approach",
        "motion"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}