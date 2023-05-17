package org.utbot.spring.postProcessors

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.warn
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.PriorityOrdered
import org.utbot.spring.exception.UtBotSpringShutdownException
import org.utbot.spring.generated.BeanAdditionalData
import org.utbot.spring.generated.BeanDefinitionData

val logger = getLogger<UtBotBeanFactoryPostProcessor>()

object UtBotBeanFactoryPostProcessor : BeanFactoryPostProcessor, PriorityOrdered {
    /**
     * Sets the priority of post processor to highest to avoid side effects from others.
     */
    override fun getOrder(): Int = PriorityOrdered.HIGHEST_PRECEDENCE

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        logger.info { "Started post-processing bean factory in UtBot" }

        val beanDefinitions = findBeanDefinitions(beanFactory)
        logger.info { "Detected ${beanDefinitions.size} bean qualified names" }

        logger.info { "Finished post-processing bean factory in UtBot" }

        destroyBeanDefinitions(beanFactory)
        throw UtBotSpringShutdownException("Finished post-processing bean factory in UtBot", beanDefinitions)
    }

    private fun findBeanDefinitions(beanFactory: ConfigurableListableBeanFactory): List<BeanDefinitionData> =
        beanFactory.beanDefinitionNames
            .mapNotNull { getBeanDefinitionData(beanFactory, it) }
            .filterNot { it.beanTypeFqn.startsWith("org.utbot.spring") }

    private fun getBeanDefinitionData(beanFactory: ConfigurableListableBeanFactory, beanName: String): BeanDefinitionData? {
        val beanDefinition = beanFactory.getBeanDefinition(beanName)

        var beanAdditionalData: BeanAdditionalData? = null
        val beanTypeFqn = if (beanDefinition is AnnotatedBeanDefinition) {
            if (beanDefinition.factoryMethodMetadata == null) {
                // there's no factoryMethod so bean is defined with @Component-like annotation rather than @Bean annotation
                // same approach isn't applicable for @Bean beans, because for them, it returns name of @Configuration class
                beanDefinition.metadata.className.also { fqn ->
                    logger.info { "Got $fqn as metadata.className for @Component-like bean: $beanName" }
                }
            } else try {
                beanAdditionalData = BeanAdditionalData(
                    beanDefinition.factoryMethodMetadata.methodName,
                    beanDefinition.factoryMethodMetadata.declaringClassName
                )
                // TODO to avoid side effects, determine beanClassName without getting bean by analyzing method
                //  defining bean, for example, by finding all its return statements and determining their common type
                //  NOTE: do not simply use return type from method signature because it may be an interface type
                beanFactory.getBean(beanName)::class.java.name.also { fqn ->
                    logger.info { "Got $fqn as runtime type for @Bean-like bean: $beanName" }
                }
            } catch (e: BeanCreationException) {
                logger.warn { "Failed to get bean: $beanName" }
                null
            }
        } else {
            beanDefinition.beanClassName.also { fqn ->
                logger.info { "Got $fqn as beanClassName for XML-like bean: $beanName" }
            }
        }

        return beanTypeFqn?.let { BeanDefinitionData(beanName, beanTypeFqn, beanAdditionalData) }
    }

    private fun destroyBeanDefinitions(beanFactory: ConfigurableListableBeanFactory) {
        for (beanDefinitionName in beanFactory.beanDefinitionNames) {
            val beanRegistry = beanFactory as BeanDefinitionRegistry
            beanRegistry.removeBeanDefinition(beanDefinitionName)
        }
    }
}