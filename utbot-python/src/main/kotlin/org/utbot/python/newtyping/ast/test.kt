package org.utbot.python.newtyping.ast

import org.parsers.python.PythonParser
import org.parsers.python.ast.Block
import org.parsers.python.ast.FunctionDefinition
import org.utbot.python.PythonArgument
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.Visitor
import org.utbot.python.newtyping.ast.visitor.hints.HintCollector
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.inference.baseline.BaselineAlgorithm
import org.utbot.python.newtyping.runmypy.getErrorNumber
import org.utbot.python.newtyping.runmypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.runmypy.setConfigFile
import org.utbot.python.utils.Cleaner
import org.utbot.python.utils.TemporaryFileManager

fun main() {
    val content = """
    def _generate_local_course(lengths, modes, max_curvature, step_size):
        p_x, p_y, p_yaw = [0.0], [0.0], [0.0]
    
        for (mode, length) in zip(modes, lengths):
            if length == 0.0:
                continue
    
            # set origin state
            origin_x, origin_y, origin_yaw = p_x[-1], p_y[-1], p_yaw[-1]
    
            current_length = step_size
            while abs(current_length + step_size) <= abs(length):
                p_x, p_y, p_yaw = _interpolate(current_length, mode, max_curvature,
                                               origin_x, origin_y, origin_yaw,
                                               p_x, p_y, p_yaw)
                current_length += step_size
    
            p_x, p_y, p_yaw = _interpolate(length, mode, max_curvature, origin_x,
                                           origin_y, origin_yaw, p_x, p_y, p_yaw)
    
        return p_x, p_y, p_yaw
    """.trimIndent()

    val root = PythonParser(content).Module()
    val x = root
}