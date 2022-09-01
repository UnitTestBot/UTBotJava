package org.utbot.examples.arrays

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
class ArraysOverwriteValueTest : UtValueTestCaseChecker(
    testClass = ArraysOverwriteValue::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testByteArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::byteArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1.toByte() },
            { before, _, r -> before != null && before.isNotEmpty() && before[0] != 0.toByte() && r == 2.toByte() },
            { before, after, r ->
                val precondition = before != null && before.isNotEmpty() && before[0] == 0.toByte()
                val postcondition = after[0] == 1.toByte() && r == 3.toByte()

                precondition && postcondition
            },
        )
    }

    @Test
    fun testShortArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::shortArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1.toByte() },
            { before, _, r -> before != null && before.isNotEmpty() && before[0] != 0.toShort() && r == 2.toByte() },
            { before, after, r ->
                val precondition = before != null && before.isNotEmpty() && before[0] == 0.toShort()
                val postcondition = after[0] == 1.toShort() && r == 3.toByte()

                precondition && postcondition
            },
        )
    }

    @Test
    fun testCharArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::charArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1.toChar() },
            { before, _, r -> before != null && before.isNotEmpty() && before[0] != 0.toChar() && r == 2.toChar() },
            { before, after, r ->
                val precondition = before != null && before.isNotEmpty() && before[0] == 0.toChar()
                val postcondition = after[0] == 1.toChar() && r == 3.toChar()

                precondition && postcondition
            },
        )
    }

    @Test
    fun testIntArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::intArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1.toByte() },
            { before, _, r -> before != null && before.isNotEmpty() && before[0] != 0 && r == 2.toByte() },
            { before, after, r ->
                before != null && before.isNotEmpty() && before[0] == 0 && after[0] == 1 && r == 3.toByte()
            },
        )
    }

    @Test
    fun testLongArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::longArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1.toLong() },
            { before, _, r -> before != null && before.isNotEmpty() && before[0] != 0.toLong() && r == 2.toLong() },
            { before, after, r ->
                val precondition = before != null && before.isNotEmpty() && before[0] == 0.toLong()
                val postcondition = after[0] == 1.toLong() && r == 3.toLong()

                precondition && postcondition
            },
        )
    }

    @Test
    fun testFloatArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::floatArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1.0f },
            { before, _, r -> before != null && before.isNotEmpty() && !before[0].isNaN() && r == 2.0f },
            { before, after, r ->
                before != null && before.isNotEmpty() && before[0].isNaN() && after[0] == 1.0f && r == 3.0f
            },
        )
    }

    @Test
    fun testDoubleArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::doubleArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1.00 },
            { before, _, r -> before != null && before.isNotEmpty() && !before[0].isNaN() && r == 2.0 },
            { before, after, r ->
                before != null && before.isNotEmpty() && before[0].isNaN() && after[0] == 1.toDouble() && r == 3.0
            },
        )
    }

    @Test
    fun testBooleanArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::booleanArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1 },
            { before, _, r -> before != null && before.isNotEmpty() && before[0] && r == 2 },
            { before, after, r -> before != null && before.isNotEmpty() && !before[0] && after[0] && r == 3 },
        )
    }

    @Test
    fun testObjectArray() {
        checkParamsMutationsAndResult(
            ArraysOverwriteValue::objectArray,
            eq(4),
            { before, _, _ -> before == null },
            { before, _, r -> before != null && before.isEmpty() && r == 1 },
            { before, _, r -> before != null && before.isNotEmpty() && before[0] == null && r == 2 },
            { before, after, r ->
                before != null && before.isNotEmpty() && before[0] != null && after[0] == null && r == 3
            },
        )
    }
}