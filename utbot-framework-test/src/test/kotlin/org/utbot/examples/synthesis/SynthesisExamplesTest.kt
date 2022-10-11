package org.utbot.examples.synthesis

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.synthesis.Synthesizer
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException

class SynthesisExamplesTest : UtValueTestCaseChecker(
    testClass = SynthesisExamples::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        // kotlin is turned off, because UtBot Kotlin code generation
        // currently does not support collections
        //  CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN)
    )
) {
    private val initialEnableSynthesizer = UtSettings.enableSynthesis
    private val initialEnableSynthesisCache = UtSettings.enableSynthesisCache
    private val initialTimeoutInMillis = UtSettings.synthesisTimeoutInMillis
    private val initialMaxDepth = UtSettings.synthesisMaxDepth

    companion object {
        private const val EPS = 1e5
    }

    @BeforeAll
    fun enableSynthesizer() {
        UtSettings.enableSynthesis = true
        UtSettings.enableSynthesisCache = true
        UtSettings.synthesisTimeoutInMillis = 100_000
        UtSettings.synthesisMaxDepth = 10
    }

    @AfterAll
    fun disableSynthesizer() {
        UtSettings.enableSynthesis = initialEnableSynthesizer
        UtSettings.enableSynthesisCache = initialEnableSynthesisCache
        UtSettings.synthesisTimeoutInMillis = initialTimeoutInMillis
        UtSettings.synthesisMaxDepth = initialMaxDepth
    }

    @BeforeEach
    fun cleanupSynthesizer() {
        Synthesizer.cleanStats()
    }

    @Test
    fun testSynthesizePoint() {
        checkWithException(
            SynthesisExamples::synthesizePoint,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeInterface() {
        checkWithException(
            SynthesisExamples::synthesizeInterface,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeList() {
        checkWithException(
            SynthesisExamples::synthesizeList,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.85)
    }

    @Test
    fun testSynthesizeSet() {
        checkWithException(
            SynthesisExamples::synthesizeSet,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.5)
    }

    @Test
    fun testSynthesizeList2() {
        checkWithException(
            SynthesisExamples::synthesizeList2,
            ignoreExecutionsNumber,
            { _, _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeObject() {
        checkWithException(
            SynthesisExamples::synthesizeObject,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeDeepComplexObject() {
        checkWithException(
            SynthesisExamples::synthesizeDeepComplexObject,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeComplexCounter() {
        checkWithException(
            SynthesisExamples::synthesizeComplexCounter,
            ignoreExecutionsNumber,
            { _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeComplexObject() {
        checkWithException(
            SynthesisExamples::synthesizeComplexObject,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeComplexCounter2() {
        checkWithException(
            SynthesisExamples::synthesizeComplexCounter2,
            ignoreExecutionsNumber,
            { _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeComplexCounter3() {
        checkWithException(
            SynthesisExamples::synthesizeComplexCounter3,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.8)
    }

    @Test
    fun testSynthesizeComplexObject2() {
        checkWithException(
            SynthesisExamples::synthesizeComplexObject2,
            ignoreExecutionsNumber,
            { _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeInt() {
        checkWithException(
            SynthesisExamples::synthesizeInt,
            ignoreExecutionsNumber,
            { _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertEquals(1.0, Synthesizer.successRate, EPS)
    }

    @Test
    fun testSynthesizeSimpleList() {
        checkWithException(
            SynthesisExamples::synthesizeSimpleList,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.8)
    }

    @Test
    fun testSynthesizeIntArray() {
        checkWithException(
            SynthesisExamples::synthesizeIntArray,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.8)
    }

    @Test
    fun testSynthesizePointArray() {
        checkWithException(
            SynthesisExamples::synthesizePointArray,
            ignoreExecutionsNumber,
            { _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.8)
    }

    @Test
    fun testSynthesizePointArray2() {
        checkWithException(
            SynthesisExamples::synthesizePointArray2,
            ignoreExecutionsNumber,
            { _, _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.8)
    }

    @Test
    fun testSynthesizeDoublePointArray() {
        checkWithException(
            SynthesisExamples::synthesizeDoublePointArray,
            ignoreExecutionsNumber,
            { _, _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.8)
    }

    @Test
    fun testSynthesizeInterfaceArray() {
        checkWithException(
            SynthesisExamples::synthesizeInterfaceArray,
            ignoreExecutionsNumber,
            { _, _, r -> r.isException<IllegalArgumentException>() },
            coverage = DoNotCalculate
        )
        assertTrue(Synthesizer.successRate > 0.9)
    }
}
