package org.utbot.spring.provider

import org.utbot.spring.api.provider.InstantiationSettings
import org.utbot.spring.dummy.DummySpringBootIntegrationTestClass
import org.utbot.spring.SpringApiImpl
import org.utbot.spring.dummy.DummySpringBootIntegrationTestClassAutoconfigMockMvc
import org.utbot.spring.dummy.DummySpringBootIntegrationTestClassAutoconfigMockMvcAndTestDB
import org.utbot.spring.dummy.DummySpringBootIntegrationTestClassAutoconfigTestDB
import org.utbot.spring.utils.DependencyUtils.isSpringBootTestOnClasspath
import org.utbot.spring.utils.DependencyUtils.isSpringDataOnClasspath
import org.utbot.spring.utils.DependencyUtils.isSpringWebOnClasspath

class SpringBootApiProvider : SpringApiProvider {

    override fun isAvailable(): Boolean = isSpringBootTestOnClasspath

    override fun provideAPI(instantiationSettings: InstantiationSettings) =
        SpringApiImpl(
            instantiationSettings,
            when {
                isSpringDataOnClasspath && isSpringWebOnClasspath -> DummySpringBootIntegrationTestClassAutoconfigMockMvcAndTestDB::class
                isSpringDataOnClasspath && !isSpringWebOnClasspath -> DummySpringBootIntegrationTestClassAutoconfigTestDB::class
                !isSpringDataOnClasspath && isSpringWebOnClasspath -> DummySpringBootIntegrationTestClassAutoconfigMockMvc::class
                else -> DummySpringBootIntegrationTestClass::class
            }.java
        )
}