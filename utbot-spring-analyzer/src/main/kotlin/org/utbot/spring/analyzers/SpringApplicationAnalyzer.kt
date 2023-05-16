package org.utbot.spring.analyzers

import org.springframework.core.env.ConfigurableEnvironment

interface SpringApplicationAnalyzer {

    fun canAnalyze(): Boolean

    fun analyze(sources: Array<Class<*>>, environment: ConfigurableEnvironment)
}
