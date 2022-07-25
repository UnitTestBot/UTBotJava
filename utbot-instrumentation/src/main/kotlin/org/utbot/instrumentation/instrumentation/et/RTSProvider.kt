package org.utbot.instrumentation.instrumentation.et

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import org.utbot.framework.plugin.api.util.utContext

/**
 * Helper class for [RuntimeTraceStorage] data transactions between ClassLoaders.
 *
 * @param targetClassLoader ClassLoader where data is stored.
 */
class RTSProvider(private val targetClassLoader: ClassLoader = utContext.classLoader) {
    val trace: LongArray by FieldDelegate(RuntimeTraceStorage::`$__trace__`.name)
    val traceCallId: IntArray by FieldDelegate(RuntimeTraceStorage::`$__trace_call_id__`.name)
    var counter: Int by FieldDelegate(RuntimeTraceStorage::`$__counter__`.name)
    var counterCallId: Int by FieldDelegate(RuntimeTraceStorage::`$__counter_call_id__`.name)

    private val rts = targetClassLoader.loadClass(RuntimeTraceStorage::class.java.name)

    private inner class FieldDelegate<R>(
        fieldName: String
    ) {
        private val field by lazy {
            rts.getField(fieldName).apply {
                isAccessible = true
            }
        }
        private var cache: R? = null

        operator fun getValue(thisRef: Any?, propery: KProperty<*>): R {
            if (cache == null) {
                @Suppress("UNCHECKED_CAST")
                cache = field.get(null) as R // this cast possible because all types are basic types of java
            }
            return cache!!
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
            field.set(null, value)
        }

        fun reset() {
            cache = null
        }
    }

    fun reset() {
        listOf(this::trace, this::traceCallId, this::counter, this::counterCallId).forEach { field ->
            (field.apply {
                isAccessible = true
            }.getDelegate() as FieldDelegate<*>).reset()
        }
    }

}
