package org.utbot.spring.instantiator

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment

interface SpringApplicationInstantiator {

    fun canInstantiate(): Boolean

    fun instantiate(sources: Array<Class<*>>, environment: ConfigurableEnvironment): ConfigurableApplicationContext
}
