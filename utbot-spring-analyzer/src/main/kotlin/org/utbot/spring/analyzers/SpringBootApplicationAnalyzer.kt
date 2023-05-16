package org.utbot.spring.analyzers

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.core.env.ConfigurableEnvironment

class SpringBootApplicationAnalyzer : SpringApplicationAnalyzer {

    override fun canAnalyze(): Boolean = try {
        this::class.java.classLoader.loadClass("org.springframework.boot.SpringApplication")
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    override fun setup(sources: Array<Class<*>>, environment: ConfigurableEnvironment): SpringCustomInfrastructure {
        val application = SpringApplicationBuilder(*sources)
            .environment(environment)
            .build()

        return SpringCustomInfrastructure(application = application)
    }

    override fun analyzeWith(infrastructure: SpringCustomInfrastructure) {
        infrastructure.application?.run()
    }
}