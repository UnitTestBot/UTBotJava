package org.utbot.spring.instantiator

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.core.env.ConfigurableEnvironment

class SpringBootApplicationInstantiator : SpringApplicationInstantiator {

    override fun canInstantiate(): Boolean = try {
        this::class.java.classLoader.loadClass("org.springframework.boot.SpringApplication")
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    override fun instantiate(sources: Array<Class<*>>, environment: ConfigurableEnvironment) {
        val application = SpringApplicationBuilder(*sources)
            .environment(environment)
            .build()

        application.run()
    }
}