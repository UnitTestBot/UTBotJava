package org.utbot.spring.api

import java.net.URL

class ApplicationData(
    val configurationFile: String,
    val fileStorage: List<URL>,
    val profileExpression: String?,
)
