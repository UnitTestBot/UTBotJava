package org.utbot.spring.utils

import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment
import org.utbot.spring.api.ApplicationData

class EnvironmentFactory(
    private val applicationData: ApplicationData
) {
    companion object {
        const val DEFAULT_PROFILE_NAME = "default"
    }

    fun createEnvironment(): ConfigurableEnvironment {
        val profilesToActivate = parseProfileExpression(applicationData.profileExpression ?: DEFAULT_PROFILE_NAME)

        val environment = StandardEnvironment()
        environment.setActiveProfiles(*profilesToActivate)

        return environment
    }

    //TODO: implement this, e.g. 'prod|web' -> listOf(prod, web)
    private fun parseProfileExpression(profileExpression: String) : Array<String> = arrayOf(profileExpression)

}