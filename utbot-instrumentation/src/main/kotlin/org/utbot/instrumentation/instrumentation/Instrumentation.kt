package org.utbot.instrumentation.instrumentation

import java.lang.instrument.ClassFileTransformer
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.process.kryo.KryoHelper
import org.utbot.instrumentation.process.generated.InstrumentedProcessModel
import org.utbot.rd.IdleWatchdog

/**
 * Abstract class for the instrumentation.
 *
 * Except these two methods, should implement [transform] function which will be used to class instrumentation.
 *
 * @param TInvocationInstrumentation the return type of `invoke` function.
 */

interface Instrumentation<out TInvocationInstrumentation> : ClassFileTransformer {
    /**
     * Invokes a method with the given [methodSignature], the declaring class of which is [clazz], with the supplied
     * [arguments] and [parameters]. Parameters are additional data, the type of which depends on the specific implementation.
     *
     * @return Result of the invocation according to the specific implementation.
     */
    fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any? = null
    ): TInvocationInstrumentation

    fun getStaticField(fieldId: FieldId): Result<*>

    fun InstrumentedProcessModel.setupAdditionalRdResponses(kryoHelper: KryoHelper, watchdog: IdleWatchdog) {}

    interface Factory<out TIResult, out TInstrumentation : Instrumentation<TIResult>> {
        val additionalRuntimeClasspath: Set<String> get() = emptySet()
        val forceDisableSandbox: Boolean get() = false

        fun create(): TInstrumentation
    }
}