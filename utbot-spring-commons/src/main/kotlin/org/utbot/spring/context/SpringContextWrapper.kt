package org.utbot.spring.context

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.utbot.spring.api.context.ContextWrapper

class SpringContextWrapper(override val context: ConfigurableApplicationContext) : ContextWrapper {
    override fun getBeanDefinition(beanName: String): BeanDefinition = context.beanFactory.getBeanDefinition(beanName)

    override fun removeBeanDefinition(beanName: String) {
        val beanDefinitionRegistry = context.beanFactory as BeanDefinitionRegistry
        beanDefinitionRegistry.removeBeanDefinition(beanName)
    }

    override fun registerBeanDefinition(beanName: String, beanDefinition: Any) {
        val beanDefinitionRegistry = context.beanFactory as BeanDefinitionRegistry
        beanDefinition as BeanDefinition
        beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition)
    }

    override fun getBean(beanName: String): Any = context.getBean(beanName)
}