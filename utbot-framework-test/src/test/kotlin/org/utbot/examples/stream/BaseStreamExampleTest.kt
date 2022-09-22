package org.utbot.examples.stream

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.Full
import org.utbot.tests.infrastructure.FullWithAssumptions
import org.utbot.tests.infrastructure.StaticsType
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete
import org.utbot.tests.infrastructure.CodeGeneration
import java.util.Optional
import java.util.stream.Stream
import kotlin.streams.toList

// TODO 1 instruction is always uncovered https://github.com/UnitTestBot/UTBotJava/issues/193
// TODO failed Kotlin compilation (generics) JIRA:1332
class BaseStreamExampleTest : UtValueTestCaseChecker(
    testClass = BaseStreamExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testReturningStreamExample() {
        withoutConcrete {
            check(
                BaseStreamExample::returningStreamExample,
                eq(2),
                // NOTE: the order of the matchers is important because Stream could be used only once
                { c, r -> c.isNotEmpty() && c == r!!.toList() },
                { c, r -> c.isEmpty() && c == r!!.toList() },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testReturningStreamAsParameterExample() {
        withoutConcrete {
            check(
                BaseStreamExample::returningStreamAsParameterExample,
                eq(1),
                { s, r -> s != null && s.toList() == r!!.toList() },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testFilterExample() {
        check(
            BaseStreamExample::filterExample,
            ignoreExecutionsNumber,
            { c, r -> null !in c && r == false },
            { c, r -> null in c && r == true },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapExample() {
        checkWithException(
            BaseStreamExample::mapExample,
            eq(2),
            { c, r -> null in c && r.isException<NullPointerException>() },
            { c, r -> r.getOrThrow().contentEquals(c.map { it * 2 }.toTypedArray()) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testFlatMapExample() {
        check(
            BaseStreamExample::flatMapExample,
            ignoreExecutionsNumber,
            { c, r -> r.contentEquals(c.flatMap { listOf(it, it) }.toTypedArray()) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    @Tag("slow")
    fun testDistinctExample() {
        check(
            BaseStreamExample::distinctExample,
            ignoreExecutionsNumber,
            { c, r -> c == c.distinct() && r == false },
            { c, r -> c != c.distinct() && r == true },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    @Tag("slow")
    // TODO slow sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    fun testSortedExample() {
        check(
            BaseStreamExample::sortedExample,
            ignoreExecutionsNumber,
            { c, r -> c.last() < c.first() && r!!.asSequence().isSorted() },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testPeekExample() {
        checkThisAndStaticsAfter(
            BaseStreamExample::peekExample,
            ignoreExecutionsNumber,
            *streamConsumerStaticsMatchers,
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testLimitExample() {
        check(
            BaseStreamExample::limitExample,
            ignoreExecutionsNumber,
            { c, r -> c.size <= 5 && c.toTypedArray().contentEquals(r) },
            { c, r -> c.size > 5 && c.take(5).toTypedArray().contentEquals(r) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSkipExample() {
        check(
            BaseStreamExample::skipExample,
            ignoreExecutionsNumber,
            { c, r -> c.size > 5 && c.drop(5).toTypedArray().contentEquals(r) },
            { c, r -> c.size <= 5 && r!!.isEmpty() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testForEachExample() {
        checkThisAndStaticsAfter(
            BaseStreamExample::forEachExample,
            eq(2),
            *streamConsumerStaticsMatchers,
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testToArrayExample() {
        check(
            BaseStreamExample::toArrayExample,
            ignoreExecutionsNumber,
            { c, r -> c.toTypedArray().contentEquals(r) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testReduceExample() {
        check(
            BaseStreamExample::reduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 42 },
            { c, r -> c.isNotEmpty() && r == c.sum() + 42 },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testOptionalReduceExample() {
        checkWithException(
            BaseStreamExample::optionalReduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == Optional.empty<Int>() },
            { c, r -> c.isNotEmpty() && c.single() == null && r.isException<NullPointerException>() },
            { c, r -> c.isNotEmpty() && r.getOrThrow() == Optional.of<Int>(c.sum()) },
            coverage = DoNotCalculate // TODO 2 instructions are uncovered https://github.com/UnitTestBot/UTBotJava/issues/193
        )
    }

    @Test
    fun testComplexReduceExample() {
        check(
            BaseStreamExample::complexReduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && c.sumOf { it.toDouble() } + 42.0 == r },
            { c: List<Int?>, r -> c.isNotEmpty() && c.sumOf { it?.toDouble() ?: 0.0 } + 42.0 == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    @Disabled("TODO zero executions https://github.com/UnitTestBot/UTBotJava/issues/207")
    fun testCollectorExample() {
        check(
            BaseStreamExample::collectorExample,
            ignoreExecutionsNumber,
            { c, r -> c.toSet() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testCollectExample() {
        checkWithException(
            BaseStreamExample::collectExample,
            ignoreExecutionsNumber, // 3 executions instead of 2 expected
            { c, r -> null in c && r.isException<NullPointerException>() },
            { c, r -> null !in c && c.sum() == r.getOrThrow() },
            coverage = DoNotCalculate // TODO 2 instructions are uncovered https://github.com/UnitTestBot/UTBotJava/issues/193
        )
    }

    @Test
    fun testMinExample() {
        checkWithException(
            BaseStreamExample::minExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == Optional.empty<Int>() },
            { c, r -> c.isNotEmpty() && c.all { it == null } && r.isException<NullPointerException>() },
            { c, r -> c.isNotEmpty() && r.getOrThrow() == Optional.of<Int>(c.minOrNull()!!) },
            coverage = DoNotCalculate // TODO 2 instructions are uncovered https://github.com/UnitTestBot/UTBotJava/issues/193
        )
    }

    @Test
    fun testMaxExample() {
        checkWithException(
            BaseStreamExample::maxExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == Optional.empty<Int>() },
            { c, r -> c.isNotEmpty() && c.all { it == null } && r.isException<NullPointerException>() },
            { c, r -> c.isNotEmpty() && r.getOrThrow() == Optional.of<Int>(c.maxOrNull()!!) },
            coverage = DoNotCalculate // TODO 2 instructions are uncovered https://github.com/UnitTestBot/UTBotJava/issues/193
        )
    }

    @Test
    fun testCountExample() {
        check(
            BaseStreamExample::countExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0L },
            { c, r -> c.isNotEmpty() && c.size.toLong() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testAnyMatchExample() {
        check(
            BaseStreamExample::anyMatchExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == false },
            { c, r -> c.isNotEmpty() && c.all { it == null } && r == true },
            { c, r -> c.isNotEmpty() && c.first() != null && c.last() == null && r == true },
            { c, r -> c.isNotEmpty() && c.first() == null && c.last() != null && r == true },
            { c, r -> c.isNotEmpty() && c.none { it == null } && r == false },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testAllMatchExample() {
        check(
            BaseStreamExample::allMatchExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == true },
            { c, r -> c.isNotEmpty() && c.all { it == null } && r == true },
            { c, r -> c.isNotEmpty() && c.first() != null && c.last() == null && r == false },
            { c, r -> c.isNotEmpty() && c.first() == null && c.last() != null && r == false },
            { c, r -> c.isNotEmpty() && c.none { it == null } && r == false },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testNoneMatchExample() {
        check(
            BaseStreamExample::noneMatchExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == true },
            { c, r -> c.isNotEmpty() && c.all { it == null } && r == false },
            { c, r -> c.isNotEmpty() && c.first() != null && c.last() == null && r == false },
            { c, r -> c.isNotEmpty() && c.first() == null && c.last() != null && r == false },
            { c, r -> c.isNotEmpty() && c.none { it == null } && r == true },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testFindFirstExample() {
        checkWithException(
            BaseStreamExample::findFirstExample,
            eq(3),
            { c, r -> c.isEmpty() && r.getOrThrow() == Optional.empty<Int>() },
            { c: List<Int?>, r -> c.isNotEmpty() && c.first() == null && r.isException<NullPointerException>() },
            { c, r -> c.isNotEmpty() && r.getOrThrow() == Optional.of(c.first()) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testIteratorExample() {
        checkWithException(
            BaseStreamExample::iteratorSumExample,
            eq(2),
            { c, r -> null in c && r.isException<NullPointerException>() },
            { c, r -> null !in c && r.getOrThrow() == c.sum() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testStreamOfExample() {
        withoutConcrete {
            check(
                BaseStreamExample::streamOfExample,
                ignoreExecutionsNumber,
                // NOTE: the order of the matchers is important because Stream could be used only once
                { c, r -> c.isNotEmpty() && c.contentEquals(r!!.toArray()) },
                { c, r -> c.isEmpty() && Stream.empty<Int>().toArray().contentEquals(r!!.toArray()) },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testClosedStreamExample() {
        checkWithException(
            BaseStreamExample::closedStreamExample,
            eq(1),
            { _, r -> r.isException<IllegalStateException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCustomCollectionStreamExample() {
        check(
            BaseStreamExample::customCollectionStreamExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0L },
            { c, r -> c.isNotEmpty() && c.size.toLong() == r },
            coverage = DoNotCalculate // TODO failed coverage calculation
        )
    }

    @Test
    fun testAnyCollectionStreamExample() {
        check(
            BaseStreamExample::anyCollectionStreamExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0L },
            { c, r -> c.isNotEmpty() && c.size.toLong() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testGenerateExample() {
        check(
            BaseStreamExample::generateExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(Array(10) { 42 }) },
            coverage = Full
        )
    }

    @Test
    fun testIterateExample() {
        check(
            BaseStreamExample::iterateExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(Array(10) { i -> 42 + i }) },
            coverage = Full
        )
    }

    @Test
    fun testConcatExample() {
        check(
            BaseStreamExample::concatExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(Array(10) { 42 } + Array(10) { i -> 42 + i }) },
            coverage = Full
        )
    }

    private val streamConsumerStaticsMatchers = arrayOf(
        { _: BaseStreamExample, c: List<Int?>, _: StaticsType, _: Int? -> null in c },
        { _: BaseStreamExample, c: List<Int?>, statics: StaticsType, r: Int? ->
            val x = statics.values.single().value as Int

            r!! + c.sumOf { it ?: 0 } == x
        }
    )
}

private fun <E : Comparable<E>> Sequence<E>.isSorted(): Boolean = zipWithNext { a, b -> a <= b }.all { it }
