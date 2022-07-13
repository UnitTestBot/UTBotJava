package org.utbot.fuzzer.primitive

import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtValueExecution
import org.utbot.framework.plugin.api.UtMethodValueTestSet
import org.utbot.fuzzer.ObsoleteTestCaseGenerator
import kotlin.Result.Companion.success
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter.Kind
import kotlin.reflect.KType

object PrimitiveFuzzer : ObsoleteTestCaseGenerator {
    override fun generate(method: UtMethod<*>, mockStrategy: MockStrategyApi): UtMethodValueTestSet<*> =
            UtMethodValueTestSet(method, executions(method.callable))
}

private fun executions(method: KCallable<Any?>) = listOf(execution(method))

private fun execution(method: KCallable<Any?>): UtValueExecution<out Any?> {
    val params = method.parameters.filter { it.kind == Kind.VALUE }.map { it.type.utValue() }
    val returnValue = success(method.returnType.utValue().value)
    return UtValueExecution(params, returnValue)
}

// TODO: we don't cover String and nullable versions of wrappers for primitive types, for instance java.lang.Integer
private fun KType.utValue(): UtConcreteValue<out Any> =
    when (val kClass = this.classifier as KClass<*>) {
        Byte::class -> UtConcreteValue(0.toByte())
        Short::class -> UtConcreteValue(0.toShort())
        Char::class -> UtConcreteValue(0.toChar())
        Int::class -> UtConcreteValue(0)
        Long::class -> UtConcreteValue(0L)
        Float::class -> UtConcreteValue(0.0f)
        Double::class -> UtConcreteValue(0.0)
        Boolean::class -> UtConcreteValue(false)
        ByteArray::class -> UtConcreteValue(byteArrayOf())
        ShortArray::class -> UtConcreteValue(shortArrayOf())
        CharArray::class -> UtConcreteValue(charArrayOf())
        IntArray::class -> UtConcreteValue(intArrayOf())
        LongArray::class -> UtConcreteValue(longArrayOf())
        FloatArray::class -> UtConcreteValue(floatArrayOf())
        DoubleArray::class -> UtConcreteValue(doubleArrayOf())
        BooleanArray::class -> UtConcreteValue(booleanArrayOf())
        else -> UtConcreteValue(null, kClass.java)
    }