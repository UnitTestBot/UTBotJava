package org.utbot.examples.stream

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.visible.UtStreamConsumingException
import org.utbot.testcheckers.eq
import org.utbot.testing.CodeGeneration
import org.utbot.testing.FullWithAssumptions
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.isException
import kotlin.streams.toList
import org.utbot.testing.asList

// TODO 1 instruction is always uncovered https://github.com/UnitTestBot/UTBotJava/issues/193
// TODO failed Kotlin compilation (generics) JIRA:1332
class StreamsAsMethodResultExampleTest : UtValueTestCaseChecker(
    testClass = StreamsAsMethodResultExample::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    ),
) {
    @Test
    fun testReturningStreamExample() {
        check(
            StreamsAsMethodResultExample::returningStreamExample,
            eq(2),
            { c, r -> c.isEmpty() && c == r!!.asList() },
            { c, r -> c.isNotEmpty() && c == r!!.asList() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testReturningIntStreamExample() {
        checkWithException(
            StreamsAsMethodResultExample::returningIntStreamExample,
            eq(3),
            { c, r -> c.isEmpty() && c == r.getOrThrow().toList() },
            { c, r -> c.isNotEmpty() && c.none { it == null } && c.toIntArray().contentEquals(r.getOrThrow().toArray()) },
            { c, r -> c.isNotEmpty() && c.any { it == null } && r.isException<UtStreamConsumingException>() },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testReturningLongStreamExample() {
        checkWithException(
            StreamsAsMethodResultExample::returningLongStreamExample,
            eq(3),
            { c, r -> c.isEmpty() && c == r.getOrThrow().toList() },
            { c, r -> c.isNotEmpty() && c.none { it == null } && c.map { it.toLong() }.toLongArray().contentEquals(r.getOrThrow().toArray()) },
            { c, r -> c.isNotEmpty() && c.any { it == null } && r.isException<UtStreamConsumingException>() },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testReturningDoubleStreamExample() {
        checkWithException(
            StreamsAsMethodResultExample::returningDoubleStreamExample,
            eq(3),
            { c, r -> c.isEmpty() && c == r.getOrThrow().toList() },
            { c, r -> c.isNotEmpty() && c.none { it == null } && c.map { it.toDouble() }.toDoubleArray().contentEquals(r.getOrThrow().toArray()) },
            { c, r -> c.isNotEmpty() && c.any { it == null } && r.isException<UtStreamConsumingException>() },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }
}
