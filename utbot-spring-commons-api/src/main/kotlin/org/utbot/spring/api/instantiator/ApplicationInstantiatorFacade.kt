package org.utbot.spring.api.instantiator

import org.utbot.spring.api.context.ContextWrapper


interface ApplicationInstantiatorFacade {
    fun instantiate(instantiationSettings: InstantiationSettings): ContextWrapper?
}