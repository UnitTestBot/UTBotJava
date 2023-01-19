package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "python3",
        directoriesForSysPath = setOf("/home/tochilinak/Documents/projects/utbot/PythonRobotics"),
        "/home/tochilinak/Documents/projects/utbot/PythonRobotics/PathPlanning/Dijkstra/dijkstra.py",
        moduleOfSourceFile = "PathPlanning.Dijkstra.dijkstra",
        ""
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}