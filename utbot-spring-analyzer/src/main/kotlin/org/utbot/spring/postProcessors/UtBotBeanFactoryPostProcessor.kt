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

    private fun findBeanClassNames(beanFactory: ConfigurableListableBeanFactory): List<String> =
        beanFactory.beanDefinitionNames.map {
            beanFactory.getBeanDefinition(it).beanClassName
                // TODO: avoid getting bean and use methodName + declaringClass from previous implementation
                //  and use it to find return type from PsiMethod return expressions in utbot main process.
                ?: beanFactory.getBean(it)::class.java.name
        }

    private fun destroyBeanDefinitions(beanFactory: ConfigurableListableBeanFactory) {
        for (beanDefinitionName in beanFactory.beanDefinitionNames) {
            val beanRegistry = beanFactory as BeanDefinitionRegistry
            beanRegistry.removeBeanDefinition(beanDefinitionName)
        }
    }
}
