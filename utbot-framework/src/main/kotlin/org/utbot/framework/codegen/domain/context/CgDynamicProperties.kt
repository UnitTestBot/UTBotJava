package org.utbot.framework.codegen.domain.context

import java.util.*

interface CgDynamicProperty<T> {
    // TODO may be useful `val scope: CgPropertyScope`
    //  (where CgPropertyScope is some enum with values CLASS, METHOD, TEST_SET, CODE_BLOCK, etc.)
}

class CgDynamicProperties {
    private val properties = IdentityHashMap<CgDynamicProperty<*>, Any?>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(property: CgDynamicProperty<T>) =
        properties.getValue(property) as T

    operator fun <T> set(property: CgDynamicProperty<T>, value: T) = properties.put(property, value)
}
