package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.util.signature
import java.lang.reflect.Method
import java.util.IdentityHashMap

/**
 * Some information, which is computed after classes instrumentation.
 *
 * This information will be used later in `invoke` function.
 */
class InstrumentationContext {
    /**
     * Contains unique id for each method, which is required for this method mocking.
     */
    val methodSignatureToId = mutableMapOf<String, Int>()

    object MockGetter {
        data class MockContainer(private val values: List<*>) {
            private var ptr: Int = 0
            fun hasNext(): Boolean = ptr < values.size
            fun nextValue(): Any? = values[ptr++]
        }

        /**
         * Instance -> method -> list of values in the return order
         */
        private val mocks = IdentityHashMap<Any?, MutableMap<String, MockContainer>>()
        private val callSites = HashMap<String, Set<String>>()

        /**
         * Returns possibility of taking mock object of method with supplied [methodSignature] on an [obj] object.
         */
        @JvmStatic
        fun hasMock(obj: Any?, methodSignature: String): Boolean =
            mocks[obj]?.get(methodSignature)?.hasNext() ?: false

        /**
         * Returns the next value for mocked method with supplied [methodSignature] on an [obj] object.
         *
         * This function has only to be called from the instrumented bytecode everytime
         * we need a next value for a mocked method.
         */
        @JvmStatic
        fun getMock(obj: Any?, methodSignature: String): Any? =
            mocks[obj]?.get(methodSignature).let { container ->
                container ?: error("Can't get mock container for method [$obj\$$methodSignature]")
                container.nextValue()
            }

        /**
         * Returns current callSites for mocking new instance of [instanceType] contains [callSite] or not
         */
        @JvmStatic
        fun checkCallSite(instanceType: String, callSite: String): Boolean {
            return callSites.getOrDefault(instanceType, emptySet()).contains(callSite)
        }

        fun updateCallSites(instanceType: String, instanceCallSites: Set<String>) {
            callSites[instanceType] = instanceCallSites
        }

        fun updateMocks(obj: Any?, methodSignature: String, values: List<*>) {
            val methodMocks = mocks.getOrPut(obj) { mutableMapOf() }
            methodMocks[methodSignature] = MockContainer(values)
        }

        fun updateMocks(obj: Any?, method: Method, values: List<*>) {
            updateMocks(obj, method.signature, values)
        }
    }
}
