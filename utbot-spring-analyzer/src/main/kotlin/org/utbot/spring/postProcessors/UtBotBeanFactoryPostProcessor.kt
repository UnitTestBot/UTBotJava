package org.utbot.spring.postProcessors

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

    private fun findBeanClassNames(beanFactory: ConfigurableListableBeanFactory): ArrayList<String> {
        val beanClassNames = ArrayList<String>()
        for (beanDefinitionName in beanFactory.beanDefinitionNames) {
            val beanDefinition = beanFactory.getBeanDefinition(beanDefinitionName)

            if (beanDefinition is AnnotatedBeanDefinition) {
                val factoryMethodMetadata = beanDefinition.factoryMethodMetadata
                if (factoryMethodMetadata != null) {
                    beanClassNames.add(factoryMethodMetadata.returnTypeName)
                }
            } else {
                var className = beanDefinition.beanClassName
                if (className == null) {
                    className = beanFactory.getBean(beanDefinitionName).javaClass.name
                }
                className?.let { beanClassNames.add(it) }
            }
        }

        return beanClassNames
    }

    private fun destroyBeanDefinitions(beanFactory: ConfigurableListableBeanFactory) {
        for (beanDefinitionName in beanFactory.beanDefinitionNames) {
            val beanRegistry = beanFactory as BeanDefinitionRegistry
            beanRegistry.removeBeanDefinition(beanDefinitionName)
        }
    }
}
