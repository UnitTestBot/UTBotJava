package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.jClass
import org.objectweb.asm.Type

class InstanceMockController(
    clazz: ClassId,
    private val instances: List<Any?>,
    private val callSites: Set<String>,
) : MockController {
    private val type = Type.getInternalName(clazz.jClass)

    override fun init() {
        MockGetter.updateCallSites(type, callSites)
        MockGetter.updateMocks(type, instances)
    }

    override fun close() {
        MockGetter.updateCallSites(type, emptySet())
    }
}