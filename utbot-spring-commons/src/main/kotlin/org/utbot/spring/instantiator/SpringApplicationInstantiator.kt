package org.utbot.spring.instantiator

import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.instantiator.InstantiationSettings

interface SpringApplicationInstantiator {

    fun canInstantiate(): Boolean

    fun instantiate(instantiationSettings: InstantiationSettings): ContextWrapper
}
