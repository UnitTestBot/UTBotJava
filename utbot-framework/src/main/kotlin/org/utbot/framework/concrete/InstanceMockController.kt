package org.utbot.framework.concrete

import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.jClass

class InstanceMockController(
    clazz: ClassId,
    instances: List<Any?>,
    callSites: Set<String>,
) : MockController {
    private val type = Type.getInternalName(clazz.jClass)

    init {
        MockGetter.updateCallSites(type, callSites)
        MockGetter.updateMocks(null, "$type.<init>", instances)
    }

    override fun close() {
        MockGetter.updateCallSites(type, emptySet())
    }
}