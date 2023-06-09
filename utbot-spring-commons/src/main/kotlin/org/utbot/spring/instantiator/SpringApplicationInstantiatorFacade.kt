package org.utbot.spring.instantiator

import org.utbot.spring.api.instantiator.InstantiationSettings
import org.utbot.spring.environment.EnvironmentFactory

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.SpringBootVersion
import org.springframework.core.SpringVersion
import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.instantiator.ApplicationInstantiatorFacade
import org.utbot.spring.api.instantiator.ConfiguredApplicationInstantiator
import org.utbot.spring.context.SpringContextWrapper

private val logger = getLogger<SpringApplicationInstantiatorFacade>()

class SpringApplicationInstantiatorFacade : ApplicationInstantiatorFacade {

    override fun instantiate(instantiationSettings: InstantiationSettings): ContextWrapper =
        instantiate(instantiationSettings) { it.instantiate() }

    override fun <T> instantiate(
        instantiationSettings: InstantiationSettings,
        instantiatorRunner: (ConfiguredApplicationInstantiator) -> T
    ): T {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }
        logger.info { "Current Spring version is: " + runCatching { SpringVersion.getVersion() }.getOrNull() }
        logger.info { "Current Spring Boot version is: " + runCatching { SpringBootVersion.getVersion() }.getOrNull() }

        val environment = EnvironmentFactory(instantiationSettings).createEnvironment()

        for (instantiator in listOf(SpringBootApplicationInstantiator(), PureSpringApplicationInstantiator())) {
            if (instantiator.canInstantiate()) {
                logger.info { "Instantiating with $instantiator" }
                try {
                    return instantiatorRunner(ConfiguredApplicationInstantiator {
                        val context = instantiator.instantiate(instantiationSettings.configurationClasses, environment)
                        SpringContextWrapper(context)
                    })
                } catch (e: Throwable) {
                    logger.error("Instantiating with $instantiator failed", e)
                }
            }
        }

        error("Failed to initialize Spring context")
    }
}
