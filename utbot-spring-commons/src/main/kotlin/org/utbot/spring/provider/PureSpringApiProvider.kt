package org.utbot.spring.provider

import org.utbot.spring.api.provider.InstantiationSettings
import org.utbot.spring.SpringApiImpl
import org.utbot.spring.dummy.DummyPureSpringIntegrationTestClass
import org.utbot.spring.dummy.DummyPureSpringIntegrationTestClassAutoconfigTestDB
import org.utbot.spring.utils.DependencyUtils.isSpringDataOnClasspath

class PureSpringApiProvider : SpringApiProvider {

    override fun isAvailable() = true

    override fun provideAPI(instantiationSettings: InstantiationSettings) =
        SpringApiImpl(
            instantiationSettings,
            if (isSpringDataOnClasspath) DummyPureSpringIntegrationTestClassAutoconfigTestDB::class.java
            else DummyPureSpringIntegrationTestClass::class.java
        )
}