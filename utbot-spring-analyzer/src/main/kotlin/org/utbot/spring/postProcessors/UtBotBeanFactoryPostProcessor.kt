package org.utbot.spring.postProcessors

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.warn
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.core.PriorityOrdered
import org.utbot.spring.exception.UtBotSpringShutdownException

val logger = getLogger<UtBotBeanFactoryPostProcessor>()

object UtBotBeanFactoryPostProcessor : BeanFactoryPostProcessor, PriorityOrdered {
    /**
     * Sets the priority of post processor to highest to avoid side effects from others.
     */
    override fun getOrder(): Int = PriorityOrdered.HIGHEST_PRECEDENCE

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        logger.info { "Started post-processing bean factory in UtBot" }

        val beanQualifiedNames = findBeanClassNames(beanFactory)
        logger.info { "Detected ${beanQualifiedNames.size} bean qualified names" }

        logger.info { "Finished post-processing bean factory in UtBot" }

        throw UtBotSpringShutdownException("Finished post-processing bean factory in UtBot", beanQualifiedNames)
    }

    private fun findBeanClassNames(beanFactory: ConfigurableListableBeanFactory): List<String> {
        return beanFactory.beanDefinitionNames.mapNotNull {
            try {
                beanFactory.getBeanDefinition(it).beanClassName ?: beanFactory.getBean(it).javaClass.name
            } catch (e: BeanCreationException) {
                logger.warn { "Failed to get bean: $it" }
                null
            }
        }.filterNot { it.startsWith("org.utbot.spring") }
         .distinct()
    }
}
