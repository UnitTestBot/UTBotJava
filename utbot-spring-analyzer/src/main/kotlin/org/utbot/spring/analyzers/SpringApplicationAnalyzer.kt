package org.utbot.spring.analyzers

import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.ConfigurableEnvironment

interface SpringApplicationAnalyzer {

    fun canAnalyze(): Boolean

    fun setup(sources: Array<Class<*>>, environment: ConfigurableEnvironment): SpringCustomInfrastructure

    fun analyzeWith(infrastructure: SpringCustomInfrastructure)
}

data class SpringCustomInfrastructure(
    val application: SpringApplication? = null,
    val context: AnnotationConfigApplicationContext? = null,
) {
    init {
        require(application != null || context != null) {
            "Spring Application must be configured with SpringBoot or simple Spring"
        }
    }
}