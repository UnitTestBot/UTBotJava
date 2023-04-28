package org.utbot.spring.repositoryWrapper

import org.springframework.beans.factory.config.BeanPostProcessor
import java.lang.reflect.Proxy

object RepositoryWrapperBeanPostProcessor : BeanPostProcessor {
    // see https://github.com/spring-projects/spring-boot/issues/7033 for reason why we post process AFTER initialization
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any =
        if (bean::class.java.interfaces.any {
            it.name == "org.springframework.data.repository.Repository"
        }) {
            Proxy.newProxyInstance(
                this::class.java.classLoader,
                bean::class.java.interfaces,
                RepositoryWrapperInvocationHandler(bean, beanName)
            )
        } else bean
}