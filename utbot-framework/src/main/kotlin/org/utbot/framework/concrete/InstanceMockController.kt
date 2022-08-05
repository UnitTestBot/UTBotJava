package org.utbot.framework.concrete

import org.utbot.jcdb.api.ClassId

class InstanceMockController(
    clazz: ClassId,
    instances: List<Any?>,
    callSites: Set<String>,
) : MockController {

    private val type = clazz.name.replace('.', '/');

    init {
        InstrumentationContext.MockGetter.updateCallSites(type, callSites)
        InstrumentationContext.MockGetter.updateMocks(null, "$type.<init>", instances)
    }

    override fun close() {
        InstrumentationContext.MockGetter.updateCallSites(type, emptySet())
    }
}