package org.utbot.spring.provider

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper
import org.utbot.spring.api.instantiator.InstantiationSettings
import org.utbot.spring.dummy.DummySpringBootIntegrationTestClass
import org.utbot.spring.SpringApiImpl

class SpringBootApiProvider : SpringApiProvider {

    override fun isAvailable(): Boolean = try {
        SpringBootTestContextBootstrapper::class.java.name
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    override fun provideAPI(instantiationSettings: InstantiationSettings) =
        SpringApiImpl(instantiationSettings, DummySpringBootIntegrationTestClass::class.java)
}