package org.utbot.spring.api.provider

import org.utbot.spring.api.SpringApi

/**
 * Stateless provider of independent [SpringApi] instances that do not have shared state,
 * meaning each [SpringApi] instance will when needed start its own Spring Application.
 */
interface SpringApiProviderFacade {
    fun provideMostSpecificAvailableApi(instantiationSettings: InstantiationSettings): ProviderResult<SpringApi>

    /**
     * [apiUser] is consequently invoked on all available (on the classpath)
     * [SpringApi] types from most specific (e.g. Spring Boot) to least specific (e.g. Pure Spring)
     * until it executes without throwing exception, then obtained result is returned.
     *
     * All exceptions are collected into [ProviderResult.exceptions].
     */
    fun <T> useMostSpecificNonFailingApi(
        instantiationSettings: InstantiationSettings,
        apiUser: (SpringApi) -> T
    ): ProviderResult<T>

    companion object {
        fun getInstance(classLoader: ClassLoader): SpringApiProviderFacade =
            classLoader
                .loadClass("org.utbot.spring.provider.SpringApiProviderFacadeImpl")
                .getConstructor()
                .newInstance() as SpringApiProviderFacade
    }

    /**
     * [result] can be a [Result.success] while [exceptions] is not empty,
     * if we failed to use most specific [SpringApi] available (e.g. SpringBoot), but
     * were able to successfully fall back to less specific [SpringApi] (e.g. PureSpring).
     */
    class ProviderResult<out T>(
        val result: Result<T>,
        val exceptions: List<Throwable>
    )
}