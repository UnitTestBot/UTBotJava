package org.utbot.framework.concrete

import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.jcdb.api.ClassId

class InstanceMockController(
    clazz: ClassId,
    instances: List<Any?>,
    callSites: Set<String>,
) : MockController {
    private val type = Type.getInternalName(clazz.jClass)

    init {
        InstrumentationContext.MockGetter.updateCallSites(type, callSites)
        InstrumentationContext.MockGetter.updateMocks(null, "$type.<init>", instances)
    }

    override fun close() {
        InstrumentationContext.MockGetter.updateCallSites(type, emptySet())
    }
}