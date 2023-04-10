package org.utbot.spring.postProcessors

import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.PriorityOrdered

object UtBotBeanFactoryPostProcessor : BeanFactoryPostProcessor, PriorityOrdered {
    var beanQualifiedNames: List<String> = emptyList()
        private set

    /**
     * Sets the priority of post processor to highest to avoid side effects from others.
     */
    override fun getOrder(): Int = PriorityOrdered.HIGHEST_PRECEDENCE

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        println("Started post-processing bean factory in UtBot")

        val beanClassNames = findBeanClassNames(beanFactory)
        beanQualifiedNames = beanClassNames.distinct()

        // After desired post-processing is completed we destroy bean definitions
        // to avoid further possible actions with beans that may be unsafe.
        destroyBeanDefinitions(beanFactory)

        println("Finished post-processing bean factory in UtBot")
    }

    private fun findBeanClassNames(beanFactory: ConfigurableListableBeanFactory): List<String> {
        return beanFactory.beanDefinitionNames.mapNotNull {
            try {
                beanFactory.getBeanDefinition(it).beanClassName
                    //TODO: avoid getting bean here, change the interface to find method and return type
                    // and obtain required information from PsiClass in UtBot after that.
                    ?: beanFactory.getBean(it).javaClass.name
            } catch (e: BeanCreationException) {
                //logger.warn { "Failed to get bean: $it" }
                null
            }
        }.filterNot { it.startsWith("org.utbot.spring") }
    }

    private fun destroyBeanDefinitions(beanFactory: ConfigurableListableBeanFactory) {
        for (beanDefinitionName in beanFactory.beanDefinitionNames) {
            val beanRegistry = beanFactory as BeanDefinitionRegistry
            beanRegistry.removeBeanDefinition(beanDefinitionName)
        }
    }
}
