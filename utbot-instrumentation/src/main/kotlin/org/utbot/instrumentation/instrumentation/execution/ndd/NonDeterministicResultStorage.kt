@file:Suppress("UNUSED")

package org.utbot.instrumentation.instrumentation.execution.ndd

import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.utContext
import java.util.*


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

    fun makeSignature(owner: String, name: String, descriptor: String): String {
        return "$owner $name$descriptor"
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

    // putParameter[type](type)
    // peakParameter[type](): type

    @JvmStatic
    fun putParameterZ(value: Boolean) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterZ(): Boolean {
        return parameters.removeLast() as Boolean
    }

    @JvmStatic
    fun putParameterB(value: Byte) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterB(): Byte {
        return parameters.removeLast() as Byte
    }

    @JvmStatic
    fun putParameterC(value: Char) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterC(): Char {
        return parameters.removeLast() as Char
    }

    @JvmStatic
    fun putParameterS(value: Short) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterS(): Short {
        return parameters.removeLast() as Short
    }

    @JvmStatic
    fun putParameterI(value: Int) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterI(): Int {
        return parameters.removeLast() as Int
    }

    @JvmStatic
    fun putParameterJ(value: Long) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterJ(): Long {
        return parameters.removeLast() as Long
    }

    @JvmStatic
    fun putParameterF(value: Float) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterF(): Float {
        return parameters.removeLast() as Float
    }

    @JvmStatic
    fun putParameterD(value: Double) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterD(): Double {
        return parameters.removeLast() as Double
    }

    @JvmStatic
    fun putParameterL(value: Any?) {
        parameters.add(value)
    }

    @JvmStatic
    fun peakParameterL(): Any? {
        return parameters.removeLast()
    }

    // storeStatic(type, sign)

    @JvmStatic
    fun storeStatic(result: Boolean, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeStatic(result: Byte, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeStatic(result: Char, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeStatic(result: Short, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeStatic(result: Int, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeStatic(result: Long, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeStatic(result: Float, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeStatic(result: Double, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeStatic(result: Any?, signature: String) {
        staticStorage.add(NDMethodResult(signature, result))
    }

    // storeCall(type, sign)

    @JvmStatic
    fun storeCall(result: Boolean, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Byte, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Char, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Short, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Int, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Long, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Float, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Double, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }

    @JvmStatic
    fun storeCall(result: Any?, signature: String) {
        callStorage.getOrPut(currentInstance) { mutableListOf() }.add(NDMethodResult(signature, result))
    }
}
