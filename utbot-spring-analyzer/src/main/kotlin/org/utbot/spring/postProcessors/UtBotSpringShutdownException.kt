package org.utbot.spring.postProcessors

/**
 * Use this exception to shutdown this application
 * when all required analysis actions are completed.
 */
class UtBotSpringShutdownException(message: String): Exception(message)