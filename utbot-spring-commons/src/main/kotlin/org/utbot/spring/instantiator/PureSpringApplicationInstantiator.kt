package org.utbot.spring.instantiator

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.ConfigurableEnvironment

class PureSpringApplicationInstantiator : SpringApplicationInstantiator {

    override fun canInstantiate() = true

    override fun instantiate(sources: Array<Class<*>>, environment: ConfigurableEnvironment): ConfigurableApplicationContext {
        val applicationContext = AnnotationConfigApplicationContext()
        applicationContext.register(*sources)
        applicationContext.environment = environment

        applicationContext.refresh()
        return applicationContext
    }
}