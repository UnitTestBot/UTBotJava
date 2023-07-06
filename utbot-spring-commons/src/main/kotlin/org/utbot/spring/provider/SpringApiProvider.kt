package org.utbot.spring.provider

import org.utbot.spring.api.SpringApi
import org.utbot.spring.api.instantiator.InstantiationSettings

interface SpringApiProvider {

    fun isAvailable(): Boolean

    fun provideAPI(instantiationSettings: InstantiationSettings): SpringApi
}
