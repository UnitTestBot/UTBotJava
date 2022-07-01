package org.utbot.framework.concrete

import java.lang.reflect.Method
import java.util.IdentityHashMap
import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.util.signature

/**
 * Helper for working with [MockGetter] using given classLoader.
 */
@Suppress("UNCHECKED_CAST")
class MockGetterProvider(classLoader: ClassLoader) {
    private val mockGetterClass = classLoader.loadClass(MockGetter::class.java.name)

    private val callSites = mockGetterClass.getField("callSites").run {
        withAccessibility { get(null) }
    } as HashMap<String, Set<String>>

    private val mocks = mockGetterClass.getField("mocks").run {
        withAccessibility { get(null) }
    } as IdentityHashMap<Any?, MutableMap<String, Any>>

    private val mockContainerClass = classLoader.loadClass(MockGetter.MockContainer::class.java.name)

    private val mockContainerConstructor = mockContainerClass.getConstructor(List::class.java)

    fun updateCallSites(instanceType: String, instanceCallSites: Set<String>) {
        callSites[instanceType] = instanceCallSites
    }

    fun updateMocks(obj: Any?, methodSignature: String, values: List<*>) {
        val methodMocks = mocks.getOrPut(obj) { mutableMapOf() }
        methodMocks[methodSignature] = mockContainerConstructor.newInstance(values)
    }

    fun updateMocks(obj: Any?, method: Method, values: List<*>) {
        updateMocks(obj, method.signature, values)
    }

}