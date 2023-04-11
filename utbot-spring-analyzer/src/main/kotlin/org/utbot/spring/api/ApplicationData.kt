package org.utbot.spring.api

import java.net.URL

class ApplicationData(
    val classpath: Array<URL>,
    val configurationFile: String,
    val fileStorage: String?,
)
