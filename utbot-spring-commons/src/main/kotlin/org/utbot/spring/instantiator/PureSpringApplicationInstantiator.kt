package org.utbot.spring.instantiator

import org.utbot.spring.api.instantiator.InstantiationSettings
import org.utbot.spring.context.BaseDummyTestClass
import org.utbot.spring.context.SpringContextWrapper

class PureSpringApplicationInstantiator : SpringApplicationInstantiator {

    override fun canInstantiate() = true

    override fun instantiate(instantiationSettings: InstantiationSettings) =
        SpringContextWrapper(instantiationSettings, BaseDummyTestClass::class.java)
}