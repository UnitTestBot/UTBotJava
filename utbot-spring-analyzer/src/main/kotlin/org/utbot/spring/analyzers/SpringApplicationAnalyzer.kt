package org.utbot.spring.analyzers

import org.springframework.core.env.ConfigurableEnvironment

interface SpringApplicationAnalyzer {
    fun analyze(sources: Array<Class<*>>, environment: ConfigurableEnvironment): List<String>
    fun canAnalyze(): Boolean
}