package org.utbot.examples

import org.utbot.examples.samples.ClassWithMultipleConstructors
import org.utbot.examples.samples.ClassWithSameMethodNames
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.InstructionCoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.instrumentation.et.ExecutionTraceInstrumentation
import org.utbot.instrumentation.instrumentation.et.convert
import org.utbot.instrumentation.instrumentation.et.function
import org.utbot.instrumentation.instrumentation.et.invoke
import org.utbot.instrumentation.instrumentation.et.pass
import org.utbot.instrumentation.instrumentation.et.ret
import org.utbot.instrumentation.withInstrumentation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestConstructors {
    lateinit var utContext: AutoCloseable

    private val CLASSPATH = ClassWithSameMethodNames::class.java.protectionDomain.codeSource.location.path

    @Test
    fun testDefaultConstructor() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            CLASSPATH
        ).use { executor ->
            val constructors = ClassWithMultipleConstructors::class.constructors
            val constr = constructors.first { it.parameters.isEmpty() }
            val res = executor.execute(constr, arrayOf())
            val checkClass = ClassWithMultipleConstructors()
            assertEquals(checkClass, res.getOrNull())
            assertFalse(checkClass === res.getOrNull())
        }
    }

    @Test
    fun testIntConstructors() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            CLASSPATH
        ).use { executor ->
            val constructors = ClassWithMultipleConstructors::class.constructors

            val constrI = constructors.first { it.signature == "<init>(I)V" }
            val resI = executor.execute(constrI, arrayOf(1))
            assertEquals(ClassWithMultipleConstructors(1), resI.getOrNull())

            val constrII = constructors.first { it.signature == "<init>(II)V" }
            val resII = executor.execute(constrII, arrayOf(1, 2))
            assertEquals(ClassWithMultipleConstructors(3), resII.getOrNull())

            val constrIII = constructors.first { it.signature == "<init>(III)V" }
            val resIII = executor.execute(constrIII, arrayOf(1, 2, 3))
            assertEquals(ClassWithMultipleConstructors(6), resIII.getOrNull())
        }
    }

    @Test
    fun testStringConstructors() {
        withInstrumentation(
            InvokeInstrumentation.Factory,
            CLASSPATH
        ) { executor ->
            val constructors = ClassWithMultipleConstructors::class.constructors

            val constrSS = constructors.first { it.parameters.size == 2 && it.signature != "<init>(II)V" }
            val resSS = executor.execute(constrSS, arrayOf("100", "23"))
            assertEquals(ClassWithMultipleConstructors(123), resSS.getOrNull())

            val constrS = constructors.first { it.parameters.size == 1 && it.signature != "<init>(I)V" }
            val resS1 = executor.execute(constrS, arrayOf("one"))
            assertEquals(ClassWithMultipleConstructors(1), resS1.getOrNull())

            val resS2 = executor.execute(constrS, arrayOf("kek"))
            assertEquals(ClassWithMultipleConstructors(-1), resS2.getOrNull())
        }
    }

    @Test
    fun testCoverageConstructor() {
        withInstrumentation(
            InstructionCoverageInstrumentation.Factory,
            CLASSPATH
        ) { executor ->
            val constructors = ClassWithMultipleConstructors::class.constructors

            val constrIII = constructors.first { it.signature == "<init>(III)V" }
            executor.execute(constrIII, arrayOf(1, 2, 3))

            val coverage = executor.collectCoverage(ClassWithMultipleConstructors::class.java)
            val method2instr = coverage.methodToInstrRange
            assertTrue(method2instr["<init>()V"]!!.minus(coverage.visitedInstrs).isEmpty())
            assertTrue(method2instr["<init>(I)V"]!!.minus(coverage.visitedInstrs).isEmpty())
            assertTrue(method2instr["<init>(II)V"]!!.minus(coverage.visitedInstrs).isEmpty())
            assertTrue(method2instr["<init>(III)V"]!!.minus(coverage.visitedInstrs).toList() == (36..40).toList())
        }
    }

    @Test
    fun testExecutionTraceConstructor() {
        withInstrumentation(
            ExecutionTraceInstrumentation.Factory,
            CLASSPATH
        ) { executor ->
            val constructors = ClassWithMultipleConstructors::class.constructors

            val constrIII = constructors.first { it.signature == "<init>(III)V" }
            val trace = executor.execute(constrIII, arrayOf(1, 2, 3))

            assertEquals(
                function("<init>(III)V") {
                    pass()
                    invoke("<init>(II)V") {
                        pass()
                        invoke("<init>(I)V") {
                            pass()
                            invoke("<init>()V") {
                                pass()
                                ret()
                            }
                            pass()
                            ret()
                        }
                        pass()
                        ret()
                    }
                    pass()
                    ret()
                },
                convert(trace)
            )
        }
    }
}