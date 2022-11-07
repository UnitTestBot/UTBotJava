package service

import com.oracle.js.parser.ir.FunctionNode
import settings.JsDynamicSettings

data class ServiceContext(
    val utbotDir: String,
    val projectPath: String,
    val filePathToInference: String,
    val parsedFile: FunctionNode,
    val settings: JsDynamicSettings,
)