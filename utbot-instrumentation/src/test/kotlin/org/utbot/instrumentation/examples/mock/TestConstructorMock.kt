package org.utbot.instrumentation.examples.mock

import org.utbot.common.withAccessibility
import org.utbot.instrumentation.samples.mock.ClassForMockConstructor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TestConstructorMock {
    private fun checkFields(instance: Any, x: Int, s: String?) {
        val xField = instance::class.java.getDeclaredField("x")
        xField.withAccessibility {
            assertEquals(x, xField.getInt(instance))
        }
        val sField = instance::class.java.getDeclaredField("s")
        sField.withAccessibility {
            assertEquals(s, sField.get(instance))
        }
    }

    @Test
    fun testMockConstructor() {
        val mockHelper = MockHelper(ClassForMockConstructor::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance1 = instrumentedClazz.constructors.first { it.parameters.isEmpty() }.newInstance()
        val instance2 = instrumentedClazz.constructors.first { it.parameters.size == 1 }.newInstance("")
        val method = instrumentedClazz.declaredMethods.first { it.name == "getNewInstance" }

        checkFields(method.invoke(instance1, null), 15, "123")

        mockHelper.withMockedConstructor(instrumentedClazz, setOf(instrumentedClazz), listOf(instance1, instance2, null)) {
            assertEquals(instance1, method.invoke(instance2, null))
            assertEquals(Object::class.java, method.invoke(instance1, "")::class.java)
            assertEquals(instance2, method.invoke(instance1, "13"))
            assertNull(method.invoke(instance1, "12"))
        }

        checkFields(method.invoke(instance1, "ok"), 10, "ok")
    }

    @Test
    fun testUnreachableCallSites() {
        val mockHelper = MockHelper(ClassForMockConstructor::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance = instrumentedClazz.constructors.first { it.parameters.isEmpty() }.newInstance()
        val method = instrumentedClazz.declaredMethods.first { it.name == "getNewInstance" }

        mockHelper.withMockedConstructor(instrumentedClazz, emptySet(), listOf(instance, instance)) {
            var result = method.invoke(instance, null)
            assertNotEquals(instance, result)
            checkFields(result, 15, "123")
            result = method.invoke(instance, "ok")
            assertNotEquals(instance, result)
            checkFields(result, 10, "ok")
        }

        mockHelper.withMockedConstructor(instrumentedClazz, setOf(Object::class.java), listOf(instance, instance)) {
            var result = method.invoke(instance, null)
            assertNotEquals(instance, result)
            checkFields(result, 15, "123")
            result = method.invoke(instance, "ok")
            assertNotEquals(instance, result)
            checkFields(result, 10, "ok")
        }

    }

    @Test
    fun testInnerClass() {
        val mockHelper = MockHelper(ClassForMockConstructor.InnerClass::class.java)
        val clazz = ClassForMockConstructor::class.java
        val instance = clazz.newInstance()
        val innerClazz = mockHelper.instrumentedClazz
        val method = innerClazz.declaredMethods.first { it.name == "getNewInstance" }
        val instance1 = innerClazz.constructors.first().newInstance(null)
        val instance2 = innerClazz.constructors.first().newInstance("ok")

        checkFields(method.invoke(instance1), 15, "123")

        mockHelper.withMockedConstructor(clazz, setOf(innerClazz), listOf(null, instance)) {
            assertNull(method.invoke(instance1))
            assertEquals(instance, method.invoke(instance1))
        }

        checkFields(method.invoke(instance2), 10, "ok")

        mockHelper.withMockedConstructor(clazz, setOf(clazz), listOf(null, null)) {
            assertNotNull(method.invoke(instance1))
            assertNotNull(method.invoke(instance2))
        }
    }

    @Test
    fun testIncompleteMocks() {
        val mockHelper = MockHelper(ClassForMockConstructor::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance1 = instrumentedClazz.constructors.first { it.parameters.isEmpty() }.newInstance()
        val instance2 = instrumentedClazz.constructors.first { it.parameters.size == 1 }.newInstance("")
        val method = instrumentedClazz.declaredMethods.first { it.name == "getNewInstance" }

        checkFields(method.invoke(instance1, null), 15, "123")

        mockHelper.withMockedConstructor(instrumentedClazz, setOf(instrumentedClazz), listOf(instance1, instance2)) {
            assertEquals(instance1, method.invoke(instance2, null))
            assertEquals(Object::class.java, method.invoke(instance1, "")::class.java)
            assertEquals(instance2, method.invoke(instance1, "13"))
            checkFields(method.invoke(instance1, "12"), 10, "12")
            checkFields(method.invoke(instance2, null), 15, "123")
        }

        checkFields(method.invoke(instance1, "ok"), 10, "ok")
    }
}
