package org.utbot.spring.exception

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.spring.generated.BeanDefinitionData

private val logger = getLogger<UtBotSpringShutdownException>()

/**
 * Use this exception to shutdown the application
 * when all required analysis actions are completed.
 */
class UtBotSpringShutdownException(
    message: String,
    val beanDefinitions: List<BeanDefinitionData>
): RuntimeException(message) {
    companion object {
        fun catch(block: () -> Unit): UtBotSpringShutdownException {
            try {
                block()
                throw IllegalStateException("UtBotSpringShutdownException has not been thrown")
            } catch (e: Throwable) {
                // Spring sometimes wraps exceptions in other exceptions, so we go over
                // all the causes to determine if UtBotSpringShutdownException was thrown
                for(cause in generateSequence(e) { it.cause })
                    if (cause is UtBotSpringShutdownException) {
                        logger.info { "UtBotSpringShutdownException has been successfully caught" }
                        return cause
                    }
                throw e
            }
        }
    }
}
