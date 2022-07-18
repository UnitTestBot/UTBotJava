package org.utbot.instrumentation.examples.mock

import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor
import org.utbot.instrumentation.instrumentation.mock.MockConfig
import java.lang.reflect.Method
import java.util.IdentityHashMap
import kotlin.reflect.jvm.javaMethod
import org.objectweb.asm.Type

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

    inline fun <reified T> withMockedMethod(method: Method, instance: Any?, mockedValues: List<*>, block: (Any?) -> T): T {
        if (method.returnType == Void.TYPE) {
            error("Can't mock function returning void!")
        }

        val sign = method.signature
        val methodId = mockClassVisitor.signatureToId[sign]

        val isMockField = instrumentedClazz.getDeclaredField(MockConfig.IS_MOCK_FIELD + methodId)
        MockGetter.updateMocks(instance, method, mockedValues)

        return isMockField.withAccessibility {
            isMockField.set(instance, true)
            val res = block(instance)
            isMockField.set(instance, false)
            res
        }
    }

    inline fun <reified T> withMockedConstructor(clazz: Class<*>, callSites: Set<Class<*>>, mockedValues: List<*>, block: () -> T): T {
        val type = Type.getInternalName(clazz)
        val sign = "$type.<init>"

        MockGetter.updateCallSites(type, callSites.map { Type.getInternalName(it) }.toSet())
        MockGetter.updateMocks(null, sign, mockedValues)

        val res = block()

        MockGetter.updateCallSites(type, emptySet())
        return res
    }

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