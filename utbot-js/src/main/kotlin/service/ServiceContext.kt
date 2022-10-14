package service

import settings.JsDynamicSettings

data class ServiceContext(
    val utbotDir: String,
    val projectPath: String,
    val filePathToInference: String,
    val trimmedFileText: String,
    val fileText: String? = null,
    val settings: JsDynamicSettings,
)