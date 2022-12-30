package org.utbot.python.newtyping.inference

fun main() {
    TypeInferenceProcessor(
        "python3",
        "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/DubinsPath/dubins_path_planner.py",
        "_interpolate"
    ).inferTypes(cancel = { false }).forEach {  }
}