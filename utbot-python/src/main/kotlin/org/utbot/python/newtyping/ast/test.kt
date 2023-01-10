package org.utbot.python.newtyping.ast

import org.parsers.python.PythonParser

fun main() {
    val content = """
    def calc_obstacle_cost(trajectory, ob, config):
        ""${'"'}
        calc obstacle cost inf: collision
        ""${'"'}
        x[0]
        x[0:1]
        x[0:1:1]
        x[1:2, 0]
        x[:]
        x[1:]
        x[:1]
        x[::]
        x[::-1]
        ox = ob[:, 0]
        oy = ob[:, 1]
        dx = trajectory[:, 0] - ox[:, None]
        dy = trajectory[:, 1] - oy[:, None]
        r = np.hypot(dx, dy)
    
        if config.robot_type == RobotType.rectangle:
            yaw = trajectory[:, 2]
            rot = np.array([[np.cos(yaw), -np.sin(yaw)], [np.sin(yaw), np.cos(yaw)]])
            rot = np.transpose(rot, [2, 0, 1])
            local_ob = ob[:, None] - trajectory[:, 0:2]
            local_ob = local_ob.reshape(-1, local_ob.shape[-1])
            local_ob = np.array([local_ob @ x for x in rot])
            local_ob = local_ob.reshape(-1, local_ob.shape[-1])
            upper_check = local_ob[:, 0] <= config.robot_length / 2
            right_check = local_ob[:, 1] <= config.robot_width / 2
            bottom_check = local_ob[:, 0] >= -config.robot_length / 2
            left_check = local_ob[:, 1] >= -config.robot_width / 2
            if (np.logical_and(np.logical_and(upper_check, right_check),
                               np.logical_and(bottom_check, left_check))).any():
                return float("Inf")
        elif config.robot_type == RobotType.circle:
            if np.array(r <= config.robot_radius).any():
                return float("Inf")
    
        min_r = np.min(r)
        return 1.0 / min_r  # OK
    """.trimIndent()

    val root = PythonParser(content).Module()
    val x = root
}