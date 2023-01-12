package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/PythonRobotics"),
        "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/DubinsPath/dubins_path_planner.py",
        moduleOfSourceFile = "PathPlanning.DubinsPath.dubins_path_planner",
        "_dubins_path_planning_from_origin"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}