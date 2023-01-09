package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3",
        "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/DubinsPath/dubins_path_planner.py",
        "_interpolate"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}