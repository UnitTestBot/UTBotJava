@file:Suppress("UNUSED")

package org.utbot.instrumentation.instrumentation.execution.ndd

import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.utContext
import java.util.IdentityHashMap

object NonDeterministicResultStorage {

    data class NDMethodResult(val signature: String, val result: Any?)
    data class NDInstanceInfo(val instanceNumber: Int, val callSite: String)

    private var currentInstance: Any? = null
    private val parameters: MutableList<Any?> = mutableListOf()

    val staticStorage: MutableList<NDMethodResult> = mutableListOf()
    val callStorage: IdentityHashMap<Any, MutableList<NDMethodResult>> = IdentityHashMap()
    val ndInstances: IdentityHashMap<Any, NDInstanceInfo> = IdentityHashMap()
    private var nextInstanceNumber = 1

    fun clear() {
        staticStorage.clear()
        callStorage.clear()
        ndInstances.clear()
        nextInstanceNumber = 1
    }

    fun methodToSignature(methodId: MethodId): String {
        return "${methodId.classId.name} ${methodId.signature}"
    }

    fun signatureToMethod(signature: String): MethodId? {
        val sign = signature.split(' ')
        val clazz = utContext.classLoader.loadClass(
            sign[0].replace('/', '.')
        ).id
        return clazz.allMethods.find { it.signature == sign[1] }
    }

    @JvmStatic
    fun registerInstance(instance: Any, callSite: String) {
        ndInstances[instance] = NDInstanceInfo(nextInstanceNumber++, callSite)
    }

    @JvmStatic
    fun saveInstance(instance: Any) {
        currentInstance = instance
    }

    @JvmStatic
    fun putParameter(value: Int) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameter(): Int {
        return parameters.removeLast() as Int
    }

    @JvmStatic
    fun storeStatic(result: Int, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Int, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }
}
