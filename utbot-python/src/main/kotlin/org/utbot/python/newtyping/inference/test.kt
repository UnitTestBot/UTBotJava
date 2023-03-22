package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.pythonTypeRepresentation

fun main() {
    TypeInferenceProcessor(
        "py",
        directoriesForSysPath = setOf("C:\\Users\\tWX1238546\\IdeaProjects\\PythonRobotics"),
        "C:\\Users\\tWX1238546\\IdeaProjects\\PythonRobotics\\PathPlanning\\Dijkstra\\dijkstra.py",
        moduleOfSourceFile = "PathPlanning.Dijkstra.dijkstra",
        "verify_node",
        className = "Dijkstra"
    ).inferTypes(cancel = { false }).forEach {
        println(it.pythonTypeRepresentation())
    }
}