package org.utbot.examples

import org.utbot.examples.samples.staticenvironment.InnerClass
import org.utbot.examples.samples.staticenvironment.MyHiddenClass
import org.utbot.examples.samples.staticenvironment.ReferenceEqualityExampleClass
import org.utbot.examples.samples.staticenvironment.StaticExampleClass
import org.utbot.examples.samples.staticenvironment.TestedClass
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.InvokeWithStaticsInstrumentation
import org.utbot.instrumentation.util.StaticEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class TestInvokeWithStaticsInstrumentation {
    lateinit var utContext: AutoCloseable

    @BeforeEach
    fun initContext() {
        utContext = UtContext.setUtContext(UtContext(ClassLoader.getSystemClassLoader()))
    }

    @AfterEach
    fun closeConctext() {
        utContext.close()
    }

    val CLASSPATH = StaticExampleClass::class.java.protectionDomain.codeSource.location.path

    @Test
    fun testIfBranches() {
        ConcreteExecutor(
            InvokeWithStaticsInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val res = it.execute(StaticExampleClass::inc, arrayOf(), null)
            assertEquals(0, res.getOrNull())

            val staticEnvironment = StaticEnvironment(
                StaticExampleClass::digit.fieldId to 5,
            )
            val resWithStatics = it.execute(StaticExampleClass::inc, arrayOf(), parameters = staticEnvironment)
            assertEquals(1, resWithStatics.getOrNull())
        }
    }

    @Test
    fun testHiddenClass1() {
        ConcreteExecutor(
            InvokeWithStaticsInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val res = it.execute(TestedClass::slomayInts, arrayOf(), null)
            assertEquals(12, res.getOrNull())

            val se = StaticEnvironment(
                TestedClass::x.fieldId to 0,
                MyHiddenClass::var0.fieldId to 0
            )
            val resWithStatics = it.execute(TestedClass::slomayInts, arrayOf(), parameters = se)
            assertEquals(2, resWithStatics.getOrNull())
        }
    }

    @Disabled("Question: What to do when user hasn't provided all the used static fields?")
    @Test
    fun testHiddenClassRepeatCall() {
        ConcreteExecutor(
            InvokeWithStaticsInstrumentation.Factory(),
            CLASSPATH
        ).use {
            val se = StaticEnvironment(
                TestedClass::x.fieldId to 0,
                MyHiddenClass::var0.fieldId to 0
            )
            val resWithStatics = it.execute(TestedClass::slomayInts, arrayOf(), parameters = se)
            assertEquals(2, resWithStatics.getOrNull())

            val resAgain = it.execute(TestedClass::slomayInts, arrayOf(), null)
            assertEquals(12, resAgain.getOrNull())
        }
    }

    @Test
    fun testReferenceEquality() {
        ConcreteExecutor(
            InvokeWithStaticsInstrumentation.Factory(),
            CLASSPATH
        ).use {

            val thisObject = ReferenceEqualityExampleClass()

            val res12 = it.execute(ReferenceEqualityExampleClass::test12, arrayOf(thisObject), null)
            val res23 = it.execute(ReferenceEqualityExampleClass::test23, arrayOf(thisObject), null)
            val res31 = it.execute(ReferenceEqualityExampleClass::test31, arrayOf(thisObject), null)

            assertEquals(true, res12.getOrNull())
            assertEquals(false, res23.getOrNull())
            assertEquals(false, res31.getOrNull())

            val ic1 = InnerClass()

            val se = StaticEnvironment(
                ReferenceEqualityExampleClass::field1.fieldId to ic1,
                ReferenceEqualityExampleClass::field2.fieldId to ic1,
                ReferenceEqualityExampleClass::field3.fieldId to ic1
            )

            val res12_2 = it.execute(ReferenceEqualityExampleClass::test12, arrayOf(thisObject), parameters = se)
            val res23_2 = it.execute(ReferenceEqualityExampleClass::test23, arrayOf(thisObject), parameters = se)
            val res31_2 = it.execute(ReferenceEqualityExampleClass::test31, arrayOf(thisObject), parameters = se)

            assertEquals(true, res12_2.getOrNull())
            assertEquals(true, res23_2.getOrNull())
            assertEquals(true, res31_2.getOrNull())

            val ic2 = InnerClass()
            val ic3 = InnerClass()

            val se2 = StaticEnvironment(
                ReferenceEqualityExampleClass::field1.fieldId to ic3,
                ReferenceEqualityExampleClass::field2.fieldId to ic2,
                ReferenceEqualityExampleClass::field3.fieldId to ic3
            )
            val res12_3 = it.execute(ReferenceEqualityExampleClass::test12, arrayOf(thisObject), parameters = se2)
            val res23_3 = it.execute(ReferenceEqualityExampleClass::test23, arrayOf(thisObject), parameters = se2)
            val res31_3 = it.execute(ReferenceEqualityExampleClass::test31, arrayOf(thisObject), parameters = se2)

            assertEquals(false, res12_3.getOrNull())
            assertEquals(false, res23_3.getOrNull())
            assertEquals(true, res31_3.getOrNull())
        }
    }
}