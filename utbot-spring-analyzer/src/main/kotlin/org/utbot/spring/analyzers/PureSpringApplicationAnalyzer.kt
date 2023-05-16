package org.utbot.spring.analyzers

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.ConfigurableEnvironment

class PureSpringApplicationAnalyzer : SpringApplicationAnalyzer {
    override fun analyze(sources: Array<Class<*>>, environment: ConfigurableEnvironment) {
        val applicationContext = AnnotationConfigApplicationContext()
        applicationContext.register(*sources)
        applicationContext.environment = environment

        applicationContext.refresh()
    }

    override fun canAnalyze() = true
}