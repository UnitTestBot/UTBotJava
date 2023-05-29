package org.utbot.spring.instantiator

import org.utbot.spring.api.instantiator.InstantiationSettings
import org.utbot.spring.environment.EnvironmentFactory

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.SpringBootVersion
import org.springframework.core.SpringVersion
import org.utbot.spring.api.instantiator.ApplicationInstantiatorFacade
import org.utbot.spring.context.SpringContextWrapper

private val logger = getLogger<SpringApplicationInstantiatorFacade>()

class SpringApplicationInstantiatorFacade : ApplicationInstantiatorFacade {

    override fun instantiate(instantiationSettings: InstantiationSettings): SpringContextWrapper? {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }
        logger.info { "Current Spring version is: " + runCatching { SpringVersion.getVersion() }.getOrNull() }
        logger.info { "Current Spring Boot version is: " + runCatching { SpringBootVersion.getVersion() }.getOrNull() }

        val environmentFactory = EnvironmentFactory(instantiationSettings)

        val suitableInstantiator =
            listOf(SpringBootApplicationInstantiator(), PureSpringApplicationInstantiator())
                .firstOrNull { it.canInstantiate() }
                ?: null.also { logger.error { "All Spring analyzers failed, using empty bean list" } }

        logger.info { "Instantiating Spring application with $suitableInstantiator" }

        val context = suitableInstantiator?.instantiate(
            instantiationSettings.configurationClasses,
            environmentFactory.createEnvironment(),
        )

        return context?.let { SpringContextWrapper(it) }
    }
}
