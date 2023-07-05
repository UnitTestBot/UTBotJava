package org.utbot.spring.api.instantiator

import org.utbot.spring.api.SpringAPI

/**
 * Stateless provider of independent [SpringAPI] instances that do not have shared state,
 * meaning each [SpringAPI] instance will when needed start its own Spring Application.
 */
interface SpringApiProviderFacade {
    fun provideMostSpecificAvailableAPI(instantiationSettings: InstantiationSettings): SpringAPI

    /**
     * [apiUser] is consequently invoked on all available (on the classpath)
     * [SpringAPI] types from most specific (e.g. Spring Boot) to least specific (e.g. Pure Spring)
     * until it executes without throwing exception, then obtained result is returned.
     */
    fun <T> useMostSpecificNonFailingAPI(
        instantiationSettings: InstantiationSettings,
        apiUser: (SpringAPI) -> T
    ): T

    companion object {
        fun getInstance(classLoader: ClassLoader): SpringApiProviderFacade =
            classLoader
                .loadClass("org.utbot.spring.provider.SpringApiProviderFacadeImpl")
                .getConstructor()
                .newInstance() as SpringApiProviderFacade
    }
}