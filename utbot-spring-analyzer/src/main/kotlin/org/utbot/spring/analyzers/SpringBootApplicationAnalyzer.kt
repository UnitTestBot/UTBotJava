package org.utbot.spring.analyzers

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.core.env.ConfigurableEnvironment
import org.utbot.spring.exception.UtBotSpringShutdownException
import org.utbot.spring.generated.BeanDefinitionData

class SpringBootApplicationAnalyzer : SpringApplicationAnalyzer {
    override fun analyze(sources: Array<Class<*>>, environment: ConfigurableEnvironment): List<BeanDefinitionData> {
        val app = SpringApplicationBuilder(*sources)
            .environment(environment)
            .build()
        return UtBotSpringShutdownException.catch { app.run() }.beanDefinitions
    }

    override fun canAnalyze(): Boolean = try {
        this::class.java.classLoader.loadClass("org.springframework.boot.SpringApplication")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}