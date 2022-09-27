package org.utbot.framework.concrete

/**
 * Some information for mocking new instance.
 */
object MockGetter {
    data class MockContainer(private val values: List<*>) {
        private var ptr: Int = 0
        fun hasNext(): Boolean = ptr < values.size
        fun nextValue(): Any? = values[ptr++]
    }

    /**
     * Class -> list of values in the return order.
     */
    private val mocks = mutableMapOf<String, MockContainer>()
    private val callSites = HashMap<String, Set<String>>()

    /**
     * Returns possibility of taking mock object of [className].
     */
    @JvmStatic
    fun hasMock(className: String): Boolean =
        mocks[className]?.hasNext() ?: false

    /**
     * Returns the next value for mocked [className].
     *
     * This function has only to be called from the instrumented bytecode everytime
     * we need a next value for a mocked new instance.
     */
    @JvmStatic
    fun getMock(className: String): Any? =
        mocks[className].let { container ->
            container ?: error("Can't get mock container for $className")
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

    fun updateMocks(className: String, values: List<*>) {
        mocks[className] = MockContainer(values)
    }
}
