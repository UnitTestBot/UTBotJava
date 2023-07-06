package org.utbot.spring.api.instantiator

import org.utbot.spring.api.SpringApi

/**
 * Stateless provider of independent [SpringApi] instances that do not have shared state,
 * meaning each [SpringApi] instance will when needed start its own Spring Application.
 */
interface SpringApiProviderFacade {
    fun provideMostSpecificAvailableApi(instantiationSettings: InstantiationSettings): SpringApi

    /**
     * [apiUser] is consequently invoked on all available (on the classpath)
     * [SpringApi] types from most specific (e.g. Spring Boot) to least specific (e.g. Pure Spring)
     * until it executes without throwing exception, then obtained result is returned.
     */
    fun <T> useMostSpecificNonFailingApi(
        instantiationSettings: InstantiationSettings,
        apiUser: (SpringApi) -> T
    ): T

    companion object {
        fun getInstance(classLoader: ClassLoader): SpringApiProviderFacade =
            classLoader
                .loadClass("org.utbot.spring.provider.SpringApiProviderFacadeImpl")
                .getConstructor()
                .newInstance() as SpringApiProviderFacade
    }
}