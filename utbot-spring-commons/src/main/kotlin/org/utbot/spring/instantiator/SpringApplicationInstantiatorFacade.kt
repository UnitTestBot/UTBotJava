package org.utbot.spring.instantiator

import org.springframework.context.ConfigurableApplicationContext
import org.utbot.spring.context.InstantiationContext
import org.utbot.spring.environment.EnvironmentFactory

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.SpringBootVersion
import org.springframework.core.SpringVersion

private val logger = getLogger<SpringApplicationInstantiatorFacade>()

class SpringApplicationInstantiatorFacade(private val instantiationContext: InstantiationContext) {

    fun instantiate(): ConfigurableApplicationContext? {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }
        logger.info { "Current Spring version is: " + runCatching { SpringVersion.getVersion() }.getOrNull() }
        logger.info { "Current Spring Boot version is: " + runCatching { SpringBootVersion.getVersion() }.getOrNull() }

        val environmentFactory = EnvironmentFactory(instantiationContext)

        val suitableInstantiator =
            listOf(SpringBootApplicationInstantiator(), PureSpringApplicationInstantiator())
                .firstOrNull { it.canInstantiate() }
                ?: null.also { logger.error { "All Spring analyzers failed, using empty bean list" } }

        logger.info { "Instantiating Spring application with $suitableInstantiator" }

        return suitableInstantiator?.instantiate(
            instantiationContext.configurationClasses,
            environmentFactory.createEnvironment(),
        )
    }
}
