package service

import com.google.javascript.rhino.Node
import settings.JsDynamicSettings

data class ServiceContext(
    val utbotDir: String,
    val projectPath: String,
    val filePathToInference: String,
    val parsedFile: Node,
    val settings: JsDynamicSettings,
)