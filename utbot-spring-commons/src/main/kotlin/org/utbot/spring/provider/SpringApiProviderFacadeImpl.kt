package org.utbot.spring.provider

import com.jetbrains.rd.util.error
import org.utbot.spring.api.provider.InstantiationSettings

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.SpringBootVersion
import org.springframework.core.SpringVersion
import org.utbot.spring.api.SpringApi
import org.utbot.spring.api.provider.SpringApiProviderFacade
import org.utbot.spring.api.provider.SpringApiProviderFacade.ProviderResult

private val logger = getLogger<SpringApiProviderFacadeImpl>()

class SpringApiProviderFacadeImpl : SpringApiProviderFacade {

    override fun provideMostSpecificAvailableApi(instantiationSettings: InstantiationSettings): ProviderResult<SpringApi> =
        useMostSpecificNonFailingApi(instantiationSettings) { api ->
            api.getOrLoadSpringApplicationContext()
            api
        }

    override fun <T> useMostSpecificNonFailingApi(
        instantiationSettings: InstantiationSettings,
        apiUser: (SpringApi) -> T
    ): ProviderResult<T> {
        logger.info { "Current Java version is: " + System.getProperty("java.version") }
        logger.info { "Current Spring version is: " + runCatching { SpringVersion.getVersion() }.getOrNull() }
        logger.info { "Current Spring Boot version is: " + runCatching { SpringBootVersion.getVersion() }.getOrNull() }
        logger.info { "InstantiationSettings: $instantiationSettings" }

        val exceptions = mutableListOf<Throwable>()

        val apiProviders = sequenceOf(SpringBootApiProvider(), PureSpringApiProvider())

        val result = apiProviders
            .filter { apiProvider -> apiProvider.isAvailable() }
            .map { apiProvider ->
                logger.info { "Using Spring API from $apiProvider" }
                val result = runCatching { apiUser(apiProvider.provideAPI(instantiationSettings)) }
                result.onFailure { e ->
                    exceptions.add(e)
                    logger.error("Using Spring API from $apiProvider failed", e)
                }
                result
            }
            .firstOrNull { it.isSuccess }
            ?: Result.failure(exceptions.first())

        return ProviderResult(result, exceptions)
    }
}
