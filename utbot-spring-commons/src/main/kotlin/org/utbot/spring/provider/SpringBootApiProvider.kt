package org.utbot.spring.provider

import org.utbot.spring.api.provider.InstantiationSettings
import org.utbot.spring.dummy.DummySpringBootIntegrationTestClass
import org.utbot.spring.SpringApiImpl
import org.utbot.spring.dummy.DummySpringBootIntegrationTestClassAutoconfigTestDB
import org.utbot.spring.utils.DependencyUtils.isSpringBootTestOnClasspath
import org.utbot.spring.utils.DependencyUtils.isSpringDataOnClasspath

class SpringBootApiProvider : SpringApiProvider {

    override fun isAvailable(): Boolean = isSpringBootTestOnClasspath

    override fun provideAPI(instantiationSettings: InstantiationSettings) =
        SpringApiImpl(
            instantiationSettings,
            if (isSpringDataOnClasspath) DummySpringBootIntegrationTestClassAutoconfigTestDB::class.java
            else DummySpringBootIntegrationTestClass::class.java
        )
}