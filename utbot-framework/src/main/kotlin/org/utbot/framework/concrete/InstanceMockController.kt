package org.utbot.framework.concrete

import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.jClass

class InstanceMockController(
    clazz: ClassId,
    instances: List<Any?>,
    callSites: Set<String>,
    private val mockGetterProvider: MockGetterProvider,
) : MockController {
    private val type = Type.getInternalName(clazz.jClass)

    init {
        mockGetterProvider.updateCallSites(type, callSites)
        mockGetterProvider.updateMocks(null, "$type.<init>", instances)
    }

    override fun close() {
        mockGetterProvider.updateCallSites(type, emptySet())
    }
}