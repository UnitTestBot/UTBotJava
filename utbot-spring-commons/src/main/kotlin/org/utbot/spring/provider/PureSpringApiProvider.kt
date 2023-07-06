package org.utbot.spring.provider

import org.utbot.spring.api.instantiator.InstantiationSettings
import org.utbot.spring.SpringApiImpl
import org.utbot.spring.dummy.DummyPureSpringIntegrationTestClass

class PureSpringApiProvider : SpringApiProvider {

    override fun isAvailable() = true

    override fun provideAPI(instantiationSettings: InstantiationSettings) =
        SpringApiImpl(instantiationSettings, DummyPureSpringIntegrationTestClass::class.java)
}