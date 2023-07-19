package org.utbot.examples

import com.jetbrains.rd.util.reactive.RdFault
import org.utbot.examples.samples.ExampleClass
import org.utbot.examples.samples.staticenvironment.StaticExampleClass
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.util.InstrumentedProcessError
import org.utbot.instrumentation.util.Isolated
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class TestIsolated {
    lateinit var utContext: AutoCloseable

    @Test
    fun testCatchTargetException() {
        val javaClass = ExampleClass::class.java
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            javaClass.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()

            val isolatedFunction = Isolated(ExampleClass::kek2, it)

            val res = isolatedFunction(testObject, 123)

            assertTrue(res.exceptionOrNull() is ArrayIndexOutOfBoundsException)
        }
    }

    @Test
    fun testWrongArgumentsException() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()
            val isolatedFunction = Isolated(ExampleClass::bar, it)

            assertDoesNotThrow {
                isolatedFunction(testObject, 1)
            }


            val exc = assertThrows<InstrumentedProcessError> {
                isolatedFunction(testObject, 1, 2, 3)
            }

            assertInstanceOf(
                RdFault::class.java,
                exc.cause!!
            )
            assertTrue((exc.cause as RdFault).reasonTypeFqn == "IllegalArgumentException")
        }
    }

    @Test
    fun testSameResult() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()

            val isolatedFunction = Isolated(ExampleClass::dependsOnFieldReturn, it)

            val res1 = isolatedFunction(testObject)
            assertEquals(2, res1.getOrNull())

            val res2 = isolatedFunction(testObject)
            assertEquals(2, res2.getOrNull())
        }
    }

    @Test
    fun testEmptyMethod() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()

            val isolatedFunction = Isolated(ExampleClass::emptyMethod, it)

            val res = isolatedFunction(testObject)

            assertEquals(Unit::class, res.getOrNull()!!::class)
        }
    }

    @Test
    fun testStaticMethodCall() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            StaticExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val isolatedFunctionInc = Isolated(StaticExampleClass::inc, it)

            val res1 = isolatedFunctionInc()
            assertEquals(0, res1.getOrNull())

            val isolatedFunctionPlus = Isolated(StaticExampleClass::plus, it)

            val res2 = isolatedFunctionPlus(5)

            assertEquals(0, res2.getOrNull())
        }
    }

    @Test
    fun testNullableMethod() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            StaticExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val isolatedFunction = Isolated(StaticExampleClass::canBeNull, it)

            val res1 = isolatedFunction(10, "123")

            assertEquals("123", res1.getOrNull())

            val res2 = isolatedFunction(0, "kek")

            assertEquals(null, res2.getOrNull())

            val res3 = isolatedFunction(1, null)

            assertEquals(null, res3.getOrNull())
        }
    }
}