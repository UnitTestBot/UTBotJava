package org.utbot.examples

import com.jetbrains.rd.util.reactive.RdFault
import org.utbot.examples.samples.ClassWithSameMethodNames
import org.utbot.examples.samples.ExampleClass
import org.utbot.examples.samples.staticenvironment.StaticExampleClass
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.util.InstrumentedProcessError
import kotlin.reflect.full.declaredMembers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestInvokeInstrumentation {
    lateinit var utContext: AutoCloseable

    @Test
    fun testCatchTargetException() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {

            val testObject = ExampleClass()

            val res = it.execute(ExampleClass::kek2, arrayOf(testObject, 123))

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
            val exc = assertThrows<InstrumentedProcessError> {
                it.execute(
                    ExampleClass::bar,
                    arrayOf(testObject, 1, 2, 3)
                )
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

            val res1 = it.execute(ExampleClass::dependsOnFieldReturn, arrayOf(testObject))
            assertEquals(2, res1.getOrNull())

            val res2 = it.execute(ExampleClass::dependsOnFieldReturn, arrayOf(testObject))
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

            val res = it.execute(ExampleClass::emptyMethod, arrayOf(testObject))

            assertEquals(Unit::class, res.getOrNull()!!::class)
        }
    }

    @Test
    fun testStaticMethodCall() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            StaticExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val res1 = it.execute(StaticExampleClass::inc, arrayOf())
            assertEquals(0, res1.getOrNull())

            val res2 = it.execute(
                StaticExampleClass::plus,
                arrayOf(5)
            )

            assertEquals(0, res2.getOrNull())
        }
    }

    @Test
    fun testNullableMethod() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            StaticExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val res1 = it.execute(
                StaticExampleClass::canBeNull,
                arrayOf(10,
                "123")
            )
            assertEquals("123", res1.getOrNull())

            val res2 = it.execute(
                StaticExampleClass::canBeNull,
                arrayOf(0,
                "kek")
            )

            assertEquals(null, res2.getOrNull())

            val res3 = it.execute(
                StaticExampleClass::canBeNull,
                arrayOf(1,
                null)
            )

            assertEquals(null, res3.getOrNull())
        }
    }

    @Test
    fun testDifferentSignaturesButSameMethodNames() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            ClassWithSameMethodNames::class.java.protectionDomain.codeSource.location.path
        ).use {
            val clazz = ClassWithSameMethodNames::class

            val sumVararg = clazz.declaredMembers.first { it.parameters.size == 1 }
            val sum2 = clazz.declaredMembers.first { it.parameters.size == 2 }
            val sum3 = clazz.declaredMembers.first { it.parameters.size == 3 }

            val resVararg = it.execute(sumVararg, arrayOf(intArrayOf(1, 2, 3, 4, 5)))
            assertEquals(Result.success(15), resVararg)

            val resSum2 = it.execute(sum2, arrayOf(1, 5))
            assertEquals(Result.success(8), resSum2)

            val resSum3 = it.execute(sum3, arrayOf(1, 5, 4))
            assertEquals(Result.success(13), resSum3)
        }
    }
}