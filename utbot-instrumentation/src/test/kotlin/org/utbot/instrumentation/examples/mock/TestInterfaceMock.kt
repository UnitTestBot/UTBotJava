package org.utbot.instrumentation.examples.mock

import org.utbot.instrumentation.samples.mock.ClassForMockInterface
import org.utbot.instrumentation.samples.mock.IProvider
import java.lang.reflect.Method
import kotlin.reflect.jvm.javaMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


@Disabled("Mocking interfaces is not supported by our instrumentation (we use Mockito for them)")
class TestInterfaceMock {
    private fun testMethod(method: Method, clazz: Class<*>, mockedValues: List<*>) {
        val mockHelper = MockHelper(method.declaringClass)
        val mockHelperClass = MockHelper(clazz)
        val instrumentedMethod = mockHelper.instrumentedClazz.declaredMethods.first { it.name == method.name }
        val instance = mockHelperClass.instrumentedClazz.newInstance()
        instrumentedMethod.isAccessible = true

        mockHelper.withMockedMethod(instrumentedMethod, instance, mockedValues) {
            mockedValues.forEach {
                System.err.println(instrumentedMethod.declaringClass.cast(instance))
                assertEquals(it, instrumentedMethod.invoke(instance))
            }
        }
    }


    @Test
    fun testInt() {
        testMethod(ClassForMockInterface::provideInt.javaMethod!!, ClassForMockInterface::class.java, listOf(3, 2, 1))
    }

    @Test
    fun testIntDefault() {
        testMethod(
            ClassForMockInterface::provideIntDefault.javaMethod!!,
            ClassForMockInterface::class.java,
            listOf(3, 2, 1)
        )
    }

    @Test
    fun testString() {
        testMethod(IProvider::provideInt.javaMethod!!, ClassForMockInterface::class.java, listOf(3, 2, 1))
    }
}