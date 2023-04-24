package org.utbot.spring.analyzers

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.utbot.spring.exception.UtBotSpringShutdownException

class PureSpringApplicationAnalyzer : SpringApplicationAnalyzer {
    override fun analyze(sources: Array<Class<*>>, environment: ConfigurableEnvironment): List<String> {
        val applicationContext = AnnotationConfigApplicationContext()
        applicationContext.register(*sources)
        applicationContext.environment = environment
        return UtBotSpringShutdownException.catch { applicationContext.refresh() }.beanQualifiedNames
    }

    override fun canAnalyze() = true
}