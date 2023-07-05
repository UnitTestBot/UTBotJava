package org.utbot.spring.api

import org.utbot.framework.plugin.api.SpringConfiguration
import java.net.URL

class ApplicationData(
    val configurationFile: SpringConfiguration,
    val fileStorage: List<URL>,
    val profileExpression: String?,
)
