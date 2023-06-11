package org.utbot.spring.api.context

interface ContextWrapper {
    val context: Any

    fun getBeanDefinition(beanName: String): Any

    fun removeBeanDefinition(beanName: String)

    fun registerBeanDefinition(beanName: String, beanDefinition: Any)

    fun getBean(beanName: String): Any
}