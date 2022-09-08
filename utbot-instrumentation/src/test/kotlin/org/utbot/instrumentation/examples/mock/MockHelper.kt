package org.utbot.instrumentation.examples.mock

import kotlin.reflect.jvm.javaMethod
import org.objectweb.asm.Type
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor

/**
 * Helper for generating tests with methods mocks.
 */
class MockHelper(
    clazz: Class<*>
) {
    var mockClassVisitor: MockClassVisitor
    var instrumentedClazz: Class<*>

    private val memoryClassLoader = object : ClassLoader() {
        private val definitions: MutableMap<String, ByteArray> = mutableMapOf()

        fun addDefinition(name: String, bytes: ByteArray) {
            definitions[name] = bytes
        }

        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            val bytes = definitions[name]
            return if (bytes != null) {
                defineClass(name, bytes, 0, bytes.size)
            } else {
                super.loadClass(name, resolve)
            }
        }
    }

    init {
        val instrumenter = Instrumenter(clazz)
        mockClassVisitor =
            instrumenter.visitClass { writer ->
                MockClassVisitor(writer, MockGetter::getMock.javaMethod!!, MockGetter::checkCallSite.javaMethod!!, MockGetter::hasMock.javaMethod!!)
            }

        memoryClassLoader.addDefinition(clazz.name, instrumenter.classByteCode)
        instrumentedClazz = memoryClassLoader.loadClass(clazz.name)
    }

    inline fun <reified T> withMockedConstructor(clazz: Class<*>, callSites: Set<Class<*>>, mockedValues: List<*>, block: () -> T): T {
        val type = Type.getInternalName(clazz)

        MockGetter.updateCallSites(type, callSites.map { Type.getInternalName(it) }.toSet())
        MockGetter.updateMocks(type, mockedValues)

        val res = block()

        MockGetter.updateCallSites(type, emptySet())
        return res
    }


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
}