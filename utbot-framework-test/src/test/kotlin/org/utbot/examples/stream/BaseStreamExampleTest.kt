package org.utbot.examples.stream

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.AtLeast
import org.utbot.testing.CodeGeneration
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.Full
import org.utbot.testing.FullWithAssumptions
import org.utbot.testing.StaticsType
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException
import java.util.Optional
import java.util.stream.Stream
import org.utbot.testing.asList

// TODO 1 instruction is always uncovered https://github.com/UnitTestBot/UTBotJava/issues/193
// TODO failed Kotlin compilation (generics) JIRA:1332
class BaseStreamExampleTest : UtValueTestCaseChecker(
    testClass = BaseStreamExample::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testReturningStreamAsParameterExample() {
        withoutConcrete {
            check(
                BaseStreamExample::returningStreamAsParameterExample,
                eq(1),
                { s, r -> s != null && s.asList() == r!!.asList() },
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
            ignoreExecutionsNumber,
            { c, r -> null in c && r.isException<NullPointerException>() },
            { c, r -> r.getOrThrow().contentEquals(c.map { it * 2 }.toTypedArray()) },
            coverage = AtLeast(90)
        )
    }

    @Test
    @Tag("slow")
    fun testMapToIntExample() {
        checkWithException(
            BaseStreamExample::mapToIntExample,
            ignoreExecutionsNumber,
            { c, r -> null in c && r.isException<NullPointerException>() },
            { c, r -> r.getOrThrow().contentEquals(c.map { it.toInt() }.toIntArray()) },
            coverage = AtLeast(90)
        )
    }

    @Test
    @Tag("slow")
    fun testMapToLongExample() {
        checkWithException(
            BaseStreamExample::mapToLongExample,
            ignoreExecutionsNumber,
            { c, r -> null in c && r.isException<NullPointerException>() },
            { c, r -> r.getOrThrow().contentEquals(c.map { it.toLong() }.toLongArray()) },
            coverage = AtLeast(90)
        )
    }

    @Test
    @Tag("slow")
    fun testMapToDoubleExample() {
        checkWithException(
            BaseStreamExample::mapToDoubleExample,
            ignoreExecutionsNumber,
            { c, r -> null in c && r.isException<NullPointerException>() },
            { c, r -> r.getOrThrow().contentEquals(c.map { it.toDouble() }.toDoubleArray()) },
            coverage = AtLeast(90)
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
    fun testFlatMapToIntExample() {
        check(
            BaseStreamExample::flatMapToIntExample,
            ignoreExecutionsNumber,
            { c, r -> r.contentEquals(c.flatMap { listOf(it?.toInt() ?: 0, it?.toInt() ?: 0) }.toIntArray()) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testFlatMapToLongExample() {
        check(
            BaseStreamExample::flatMapToLongExample,
            ignoreExecutionsNumber,
            { c, r -> r.contentEquals(c.flatMap { listOf(it?.toLong() ?: 0L, it?.toLong() ?: 0L) }.toLongArray()) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testFlatMapToDoubleExample() {
        check(
            BaseStreamExample::flatMapToDoubleExample,
            ignoreExecutionsNumber,
            { c, r -> r.contentEquals(c.flatMap { listOf(it?.toDouble() ?: 0.0, it?.toDouble() ?: 0.0) }.toDoubleArray()) },
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
            ignoreExecutionsNumber,
            *streamConsumerStaticsMatchers,
            coverage = AtLeast(92)
        )
    }

    @Test
    fun testToArrayExample() {
        check(
            BaseStreamExample::toArrayExample,
            eq(2),
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
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == 0 },
            { c, r -> null in c && r.isException<NullPointerException>() },
            { c, r -> c.isNotEmpty() && null !in c && r.getOrThrow() == c.sum() },
            coverage = AtLeast(75)
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
}

internal val streamConsumerStaticsMatchers = arrayOf(
    { _: Any, c: List<Int?>, _: StaticsType, _: Int? -> null in c },
    { _: Any, c: List<Int?>, statics: StaticsType, r: Int? ->
        val x = statics.values.single().value as Int

        r!! + c.sumOf { it ?: 0 } == x
    }
)

internal fun <E : Comparable<E>> Sequence<E>.isSorted(): Boolean = zipWithNext { a, b -> a <= b }.all { it }
