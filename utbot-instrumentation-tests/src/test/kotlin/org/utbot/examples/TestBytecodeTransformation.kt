package org.utbot.examples

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.utbot.examples.samples.transformation.StringMethodsCalls
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.transformation.BytecodeTransformation
import org.utbot.instrumentation.withInstrumentation

class TestBytecodeTransformation {
    lateinit var utContext: AutoCloseable

    @Test
    fun testStringEqualsWithEmptyStringCall() {
        withInstrumentation(
            BytecodeTransformation.Factory,
            StringMethodsCalls::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val res1 = executor.execute(StringMethodsCalls::equalsWithEmptyString, arrayOf(""))
            assertTrue(res1.getOrNull() as Boolean)

            val res2 = executor.execute(StringMethodsCalls::equalsWithEmptyString, arrayOf("abc"))
            assertFalse(res2.getOrNull() as Boolean)

            val res3 = executor.execute(StringMethodsCalls::equalsWithEmptyString, arrayOf(null))
            assertTrue(res3.exceptionOrNull() is NullPointerException)
        }
    }

    @Test
    fun testStringEqualsWithNotEmptyStringCall() {
        withInstrumentation(
            BytecodeTransformation.Factory,
            StringMethodsCalls::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val res1 = executor.execute(StringMethodsCalls::equalsWithNotEmptyString, arrayOf(""))
            assertFalse(res1.getOrNull() as Boolean)

            val res2 = executor.execute(StringMethodsCalls::equalsWithNotEmptyString, arrayOf("abc"))
            assertTrue(res2.getOrNull() as Boolean)

            val res3 = executor.execute(StringMethodsCalls::equalsWithNotEmptyString, arrayOf("abcd"))
            assertFalse(res3.getOrNull() as Boolean)

            val res4 = executor.execute(StringMethodsCalls::equalsWithNotEmptyString, arrayOf(null))
            assertTrue(res4.exceptionOrNull() is NullPointerException)
        }
    }

    @Test
    fun testStringStartsWithWithEmptyStringCall() {
        withInstrumentation(
            BytecodeTransformation.Factory,
            StringMethodsCalls::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val res1 = executor.execute(StringMethodsCalls::startsWithWithEmptyString, arrayOf(""))
            assertTrue(res1.getOrNull() as Boolean)

            val res2 = executor.execute(StringMethodsCalls::startsWithWithEmptyString, arrayOf("abc"))
            assertTrue(res2.getOrNull() as Boolean)

            val res3 = executor.execute(StringMethodsCalls::startsWithWithEmptyString, arrayOf(null))
            assertTrue(res3.exceptionOrNull() is NullPointerException)
        }
    }

    @Test
    fun testStringStartsWithWithNotEmptyStringCall() {
        withInstrumentation(
            BytecodeTransformation.Factory,
            StringMethodsCalls::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val res1 = executor.execute(StringMethodsCalls::startsWithWithNotEmptyString, arrayOf(""))
            assertFalse(res1.getOrNull() as Boolean)

            val res2 = executor.execute(StringMethodsCalls::startsWithWithNotEmptyString, arrayOf("abc"))
            assertTrue(res2.getOrNull() as Boolean)

            val res3 = executor.execute(StringMethodsCalls::startsWithWithNotEmptyString, arrayOf("abcd"))
            assertTrue(res3.getOrNull() as Boolean)

            val res4 = executor.execute(StringMethodsCalls::startsWithWithNotEmptyString, arrayOf("aabc"))
            assertFalse(res4.getOrNull() as Boolean)

            val res5 = executor.execute(StringMethodsCalls::startsWithWithNotEmptyString, arrayOf(null))
            assertTrue(res5.exceptionOrNull() is NullPointerException)
        }
    }

    @Test
    fun testStringEndsWithWithEmptyString() {
        withInstrumentation(
            BytecodeTransformation.Factory,
            StringMethodsCalls::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val res1 = executor.execute(StringMethodsCalls::endsWithWithEmptyString, arrayOf(""))
            assertTrue(res1.getOrNull() as Boolean)

            val res2 = executor.execute(StringMethodsCalls::endsWithWithEmptyString, arrayOf("abc"))
            assertTrue(res2.getOrNull() as Boolean)

            val res3 = executor.execute(StringMethodsCalls::endsWithWithEmptyString, arrayOf(null))
            assertTrue(res3.exceptionOrNull() is NullPointerException)
        }
    }

    @Test
    fun testStringEndsWithWithNotEmptyString() {
        withInstrumentation(
            BytecodeTransformation.Factory,
            StringMethodsCalls::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val res1 = executor.execute(StringMethodsCalls::endsWithWithNotEmptyString, arrayOf(""))
            assertFalse(res1.getOrNull() as Boolean)

            val res2 = executor.execute(StringMethodsCalls::endsWithWithNotEmptyString, arrayOf("abc"))
            assertTrue(res2.getOrNull() as Boolean)

            val res3 = executor.execute(StringMethodsCalls::endsWithWithNotEmptyString, arrayOf("aabc"))
            assertTrue(res3.getOrNull() as Boolean)

            val res4 = executor.execute(StringMethodsCalls::endsWithWithNotEmptyString, arrayOf("abcd"))
            assertFalse(res4.getOrNull() as Boolean)

            val res5 = executor.execute(StringMethodsCalls::endsWithWithNotEmptyString, arrayOf(null))
            assertTrue(res5.exceptionOrNull() is NullPointerException)
        }
    }
}