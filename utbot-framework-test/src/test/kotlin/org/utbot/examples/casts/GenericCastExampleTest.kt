package org.utbot.examples.casts

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
internal class GenericCastExampleTest : UtValueTestCaseChecker(
    testClass = GenericCastExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testCompareTwoNumbers() {
        check(
            GenericCastExample::compareTwoNumbers,
            eq(5),
            { a, _, _ -> a == null },
            { _, b, _ -> b == null },
            { _, b, _ -> b.comparableGenericField == null },
            { a, b, r -> a >= b.comparableGenericField && r == 1 },
            { a, b, r -> a < b.comparableGenericField && r == -1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetGenericFieldValue() {
        check(
            GenericCastExample::getGenericFieldValue,
            eq(3),
            { g, _ -> g == null },
            { g, _ -> g.genericField == null },
            { g, r -> g?.genericField != null && r == g.genericField },
        )
    }

    @Test
    fun testCompareGenericField() {
        check(
            GenericCastExample::compareGenericField,
            between(4..5),
            { g, _, _ -> g == null },
            { g, v, _ -> g != null && v == null },
            { g, v, r -> v != null && v != g.comparableGenericField && r == -1 },
            { g, v, r -> g.comparableGenericField is Int && v != null && v == g.comparableGenericField && r == 1 },
            coverage = DoNotCalculate // TODO because of kryo exception: Buffer underflow.
        )
    }

    @Test
    fun testCreateNewGenericObject() {
        check(
            GenericCastExample::createNewGenericObject,
            eq(1),
            { r -> r is Int && r == 10 },
        )
    }

    @Test
    fun testSumFromArrayOfGenerics() {
        check(
            GenericCastExample::sumFromArrayOfGenerics,
            eq(7),
            { g, _ -> g == null },
            { g, _ -> g.genericArray == null },
            { g, _ -> g.genericArray.isEmpty() },
            { g, _ -> g.genericArray.size == 1 },
            { g, _ -> g.genericArray[0] == null },
            { g, _ -> g.genericArray[0] != null && g.genericArray[1] == null },
            { g, r -> g.genericArray[0] != null && g.genericArray[1] != null && r == g.genericArray[0] + g.genericArray[1] },
        )
    }
}