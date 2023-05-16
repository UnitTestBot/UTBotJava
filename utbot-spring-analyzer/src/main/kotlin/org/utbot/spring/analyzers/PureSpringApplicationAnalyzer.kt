package org.utbot.spring.analyzers

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.ConfigurableEnvironment

class PureSpringApplicationAnalyzer : SpringApplicationAnalyzer {

    override fun canAnalyze() = true

    override fun setup(sources: Array<Class<*>>, environment: ConfigurableEnvironment): SpringCustomInfrastructure {
        val applicationContext = AnnotationConfigApplicationContext()
        applicationContext.register(*sources)
        applicationContext.environment = environment

        return SpringCustomInfrastructure(context = applicationContext)
    }

    override fun analyzeWith(infrastructure: SpringCustomInfrastructure) {
        infrastructure.context?.refresh()
    }
}