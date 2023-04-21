package org.utbot.spring.exception

/**
 * Use this exception to shutdown the application
 * when all required analysis actions are completed.
 */
class UtBotSpringShutdownException(message: String): RuntimeException(message)
