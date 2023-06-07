package org.utbot.spring.api.instantiator

import org.utbot.spring.api.context.ContextWrapper

fun interface ConfiguredApplicationInstantiator {
    fun instantiate(): ContextWrapper
}