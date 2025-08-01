package org.utbot.examples

import org.utbot.examples.samples.ClassWithInnerClasses
import org.utbot.examples.samples.ClassWithSameMethodNames
import org.utbot.examples.samples.staticenvironment.StaticExampleClass
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.InstructionCoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.withInstrumentation
import kotlin.reflect.full.declaredMembers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestWithInstrumentation {
    lateinit var utContext: AutoCloseable

    @Test
    fun testStaticMethodCall() {
        withInstrumentation(
            InstructionCoverageInstrumentation.Factory,
            StaticExampleClass::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val res1 = executor.execute(StaticExampleClass::inc, arrayOf())
            val coverageInfo1 = executor.collectCoverage(StaticExampleClass::class.java)

            assertEquals(0, res1.getOrNull())
            assertEquals((3..6).toList() + (9..22).toList(), coverageInfo1.visitedInstrs)

            val res2 = executor.execute(
                StaticExampleClass::plus,
                arrayOf(5)
            )
            val coverageInfo2 = executor.collectCoverage(StaticExampleClass::class.java)

            assertEquals(0, res2.getOrNull())
            Assertions.assertTrue(
                coverageInfo2.methodToInstrRange[StaticExampleClass::plus.signature]!!.toList()
                    .subtract(coverageInfo2.visitedInstrs)
                    .isEmpty()
            )
        }
    }

    @Test
    fun testDifferentSignaturesButSameMethodNames() {
        withInstrumentation(
            InvokeInstrumentation.Factory,
            ClassWithSameMethodNames::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val clazz = ClassWithSameMethodNames::class

            val sumVararg = clazz.declaredMembers.first { it.parameters.size == 1 }
            val sum2 = clazz.declaredMembers.first { it.parameters.size == 2 }
            val sum3 = clazz.declaredMembers.first { it.parameters.size == 3 }

            val resVararg = executor.execute(sumVararg, arrayOf(intArrayOf(1, 2, 3, 4, 5)))
            assertEquals(Result.success(15), resVararg)

            val resSum2 = executor.execute(sum2, arrayOf(1, 5))
            assertEquals(Result.success(8), resSum2)

            val resSum3 = executor.execute(sum3, arrayOf(1, 5, 4))
            assertEquals(Result.success(13), resSum3)
        }
    }

    @Test
    fun testInnerClasses() {
        withInstrumentation(
            InstructionCoverageInstrumentation.Factory,
            ClassWithInnerClasses::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val innerClazz = ClassWithInnerClasses.InnerClass::class.java
            val innerStaticClazz = ClassWithInnerClasses.InnerStaticClass::class.java

            val classWithInnerClasses = ClassWithInnerClasses(5)
            val res = executor.execute(ClassWithInnerClasses::doSomething, arrayOf(classWithInnerClasses, 1, 1))
            assertEquals(8, res.getOrNull())

            val coverageInnerClass = executor.collectCoverage(innerClazz)
            assertEquals((0..24).toList().minus(listOf(11, 12)), coverageInnerClass.visitedInstrs)

            val coverageInnerStaticClazz = executor.collectCoverage(innerStaticClazz)
            assertEquals((3..6).toList(), coverageInnerStaticClazz.visitedInstrs)
        }
    }
}