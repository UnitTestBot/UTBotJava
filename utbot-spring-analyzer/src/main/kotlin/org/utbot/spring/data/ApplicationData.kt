package org.utbot.spring.data

import java.net.URL

data class ApplicationData(
    val classpath: Array<URL>,
    val configurationFile: String,
    val fileStorage: String?,
)
