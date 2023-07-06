package org.utbot.spring.provider

import com.jetbrains.rd.util.error
import org.utbot.spring.api.instantiator.InstantiationSettings

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.SpringBootVersion
import org.springframework.core.SpringVersion
import org.utbot.spring.api.SpringApi
import org.utbot.spring.api.instantiator.SpringApiProviderFacade

private val logger = getLogger<SpringApiProviderFacadeImpl>()

class SpringApiProviderFacadeImpl : SpringApiProviderFacade {

    override fun provideMostSpecificAvailableApi(instantiationSettings: InstantiationSettings): SpringApi =
        useMostSpecificNonFailingApi(instantiationSettings) { api ->
            api.getOrLoadSpringApplicationContext()
            api
        }

    override fun <T> useMostSpecificNonFailingApi(
        instantiationSettings: InstantiationSettings,
        apiUser: (SpringApi) -> T
    ): T {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }
        logger.info { "Current Spring version is: " + runCatching { SpringVersion.getVersion() }.getOrNull() }
        logger.info { "Current Spring Boot version is: " + runCatching { SpringBootVersion.getVersion() }.getOrNull() }

        for (apiProvider in listOf(SpringBootApiProvider(), PureSpringApiProvider())) {
            if (apiProvider.isAvailable()) {
                logger.info { "Getting Spring API from $apiProvider" }
                try {
                    return apiUser(apiProvider.provideAPI(instantiationSettings))
                } catch (e: Throwable) {
                    logger.error("Getting Spring API from $apiProvider failed", e)
                }
            }
        }

        error("Failed to use any Spring API")
    }
}
