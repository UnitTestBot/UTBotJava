package org.utbot.spring.analyzers

import org.springframework.core.env.ConfigurableEnvironment
import org.utbot.spring.utils.EnvironmentFactory

class SpringApplicationAnalysisContext(
    val sources: Array<Class<*>>,
    private val environmentFactory: EnvironmentFactory
) {
    fun createEnvironment(): ConfigurableEnvironment = environmentFactory.createEnvironment()
}