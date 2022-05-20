package org.utbot.instrumentation.examples.mock

import org.utbot.common.withAccessibility
import org.utbot.instrumentation.samples.mock.ClassForMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TestIncompleteMock {
    @Test
    fun testClassForMock_getX() {
        val mockHelper = MockHelper(ClassForMock::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance = instrumentedClazz.constructors.first { it.parameters.isEmpty() }.newInstance()
        val method = instrumentedClazz.declaredMethods.first { it.name == "getX" }

        val defaultValue = 15
        assertEquals(defaultValue, method.invoke(instance))

        val mockValues = listOf(1, 2, 3, 4, 5)
        mockHelper.withMockedMethod(method, instance, mockValues) {
            mockValues.forEach {
                assertEquals(it, method.invoke(instance))
            }
            // must call real function
            (1..5).forEach { _ ->
                assertEquals(defaultValue, method.invoke(instance))
            }
        }
    }

    @Test
    fun getClassForMock_getX_multipleCalls() {
        val mockHelper = MockHelper(ClassForMock::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance = instrumentedClazz.constructors.first { it.parameters.isEmpty() }.newInstance()
        val method = instrumentedClazz.declaredMethods.first { it.name == "getX" }
        val xField = instrumentedClazz.getDeclaredField("x")

        var currentX = 15
        assertEquals(currentX, method.invoke(instance))

        val mockValues1 = listOf(1, 2, 3, 4, 5)
        mockHelper.withMockedMethod(method, instance, mockValues1) {
            mockValues1.forEach {
                assertEquals(it, method.invoke(instance))
            }
            (1..5).forEach { value ->
                currentX = value
                xField.apply {
                    withAccessibility {
                        setInt(instance, value)
                    }
                }
                assertEquals(currentX, method.invoke(instance))
            }
        }

        assertEquals(currentX, method.invoke(instance)) // should be the same as it was before mocking

        val mockValues2 = listOf(-1, -2, -3, -4, -5)
        mockHelper.withMockedMethod(method, instance, mockValues2) {
            mockValues2.forEach {
                assertEquals(it, method.invoke(instance))
            }
            (1..5).forEach { _ ->
                assertEquals(currentX, method.invoke(instance))
            }
        }
    }

    @Test
    fun testClassForMock_getString() {
        val mockHelper = MockHelper(ClassForMock::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance = null // static method
        val method = instrumentedClazz.declaredMethods.first { it.name == "getString" }

        val defaultValue = "string"
        assertEquals(defaultValue, method.invoke(instance))

        val mockValues = listOf("a", "b", "c", "aa", "", "aaaaaa")
        mockHelper.withMockedMethod(method, instance, mockValues) {
            mockValues.forEach {
                assertEquals(it, method.invoke(instance))
            }
            (1..5).forEach { _ ->
                assertEquals(defaultValue, method.invoke(instance))
            }
        }
    }

    @Test
    fun testClassForMock_complicateMethod() {
        val mockHelper = MockHelper(ClassForMock::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance = instrumentedClazz.constructors.first { it.parameters.isEmpty() }.newInstance()
        val method = instrumentedClazz.declaredMethods.first { it.name == "complicatedMethod" }

        assertEquals("x + y == z", method.invoke(instance, 1, 2, 3, null))

        val mockValues1 = listOf("azaza", "lol", "ha", "kek")
        mockHelper.withMockedMethod(method, instance, mockValues1) {
            mockValues1.forEach {
                assertEquals(it, method.invoke(instance, 1, 2, 3, instance))
            }
            assertEquals("none", method.invoke(instance, 1, 2, 6, null))
            assertEquals("x + y == z", method.invoke(instance, 1, 2, 3, null))
            assertEquals("equals", method.invoke(instance, 0, 0, 0, instance))
        }

        assertEquals("equals", method.invoke(instance, 0, 0, 0, instance))

        val mockValues2 = listOf("ok", "ok", "", "da")
        mockHelper.withMockedMethod(method, instance, mockValues2) {
            mockValues2.forEach {
                assertEquals(it, method.invoke(instance, 1, 2, 3, instance))
            }
            assertEquals("none", method.invoke(instance, 1, 2, 6, null))
            assertEquals("x + y == z", method.invoke(instance, 1, 2, 3, null))
            assertEquals("equals", method.invoke(instance, 0, 0, 0, instance))
        }
    }

    @Test
    fun testClassForMock_provideInt_mocksBoth() {
        val mockHelper = MockHelper(ClassForMock::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance1 = instrumentedClazz.constructors.first { it.parameters.isEmpty() }.newInstance()
        val instance2 = instrumentedClazz.constructors.first { it.parameters.size == 1 }.newInstance("")
        val method = instrumentedClazz.declaredMethods.first { it.name == "provideInt" }

        assertNotEquals(method.invoke(instance1), method.invoke(instance2))

        mockHelper.withMockedMethod(method, instance1, listOf(3, 3)) {
            mockHelper.withMockedMethod(method, instance2, listOf(1, 3)) {
                assertNotEquals(method.invoke(instance1), method.invoke(instance2))
                assertEquals(method.invoke(instance1), method.invoke(instance2))
                assertEquals(15, method.invoke(instance1))
                assertEquals(10, method.invoke(instance2))
            }
        }
    }

    @Test
    fun testClassForMock_check_usesMockedValues() {
        val mockHelper = MockHelper(ClassForMock::class.java)
        val instrumentedClazz = mockHelper.instrumentedClazz
        val instance1 = instrumentedClazz.constructors.first { it.parameters.isEmpty() }.newInstance()
        val instance2 = instrumentedClazz.constructors.first { it.parameters.size == 1 }.newInstance("")
        val method = instrumentedClazz.declaredMethods.first { it.name == "provideInt" }
        val methodCheck = instrumentedClazz.declaredMethods.first { it.name == "check" }

        assertNotEquals(method.invoke(instance1), method.invoke(instance2))

        mockHelper.withMockedMethod(method, instance1, listOf(3, 3)) {
            mockHelper.withMockedMethod(method, instance2, listOf(1, 3)) {
                assertEquals(false, methodCheck.invoke(instance1, instance2))
                assertEquals(true, methodCheck.invoke(instance1, instance2))
                assertEquals(false, methodCheck.invoke(instance1, instance2))
            }
        }
    }
}