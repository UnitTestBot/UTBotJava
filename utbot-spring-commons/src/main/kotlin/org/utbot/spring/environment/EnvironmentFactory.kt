package org.utbot.spring.environment

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.StandardEnvironment
import org.utbot.spring.context.InstantiationContext


private val logger = getLogger<EnvironmentFactory>()

class EnvironmentFactory(
    private val instantiationContext: InstantiationContext
) {
    companion object {
        const val DEFAULT_PROFILE_NAME = "default"
    }

    fun createEnvironment(): ConfigurableEnvironment {
        val profilesToActivate = parseProfileExpression(instantiationContext.profileExpression)

        val environment = StandardEnvironment()

        try {
            environment.setActiveProfiles(*profilesToActivate)
        } catch (e: Exception) {
            logger.info { "Setting ${instantiationContext.profileExpression} as active profiles failed with exception $e" }
        }

        return environment
    }

    /*
     * Transforms active profile information
     * from the form of user input to a list of active profiles.
     *
     * Current user input form is comma-separated values, but it may be changed later.
     */
    private fun parseProfileExpression(profileExpression: String?): Array<String> {
        if (profileExpression.isNullOrEmpty()) {
            return arrayOf(DEFAULT_PROFILE_NAME)
        }

        return profileExpression
            .filter { !it.isWhitespace() }
            .split(',')
            .toTypedArray()
    }
}