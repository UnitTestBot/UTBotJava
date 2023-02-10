package org.utbot.python.newtyping.ast

import org.parsers.python.PythonParser

fun main() {
    val content = """
    def calc_to_goal_cost(trajectory, goal):
        ""${'"'}
            calc to goal cost with angle difference
        ""${'"'}
    
        dx = goal[0] - trajectory[-1, 0]
        dy = goal[1] - trajectory[-1, 1]
        error_angle = math.atan2(dy, dx)
        cost_angle = error_angle - trajectory[-1, 2]
        cost = abs(math.atan2(math.sin(cost_angle), math.cos(cost_angle)))
    
        return cost
    """.trimIndent()

    val root = PythonParser(content).Module()
    val x = root
}