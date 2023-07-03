package org.utbot.spring.instantiator

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper
import org.utbot.spring.api.instantiator.InstantiationSettings
import org.utbot.spring.context.SpringBootDummyTestClass
import org.utbot.spring.context.SpringContextWrapper

class SpringBootApplicationInstantiator : SpringApplicationInstantiator {

    override fun canInstantiate(): Boolean = try {
        SpringBootTestContextBootstrapper::class.java.name
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    override fun instantiate(instantiationSettings: InstantiationSettings) =
        SpringContextWrapper(instantiationSettings, SpringBootDummyTestClass::class.java)
}