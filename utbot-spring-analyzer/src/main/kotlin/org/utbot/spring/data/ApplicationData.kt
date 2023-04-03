package org.utbot.spring.data

import java.net.URL

data class ApplicationData(
    val applicationUrlArray: Array<URL>,
    val configurationFile: String,
    val propertyFilesPaths: List<String>,
    val xmlConfigurationPaths: List<String>,
)
