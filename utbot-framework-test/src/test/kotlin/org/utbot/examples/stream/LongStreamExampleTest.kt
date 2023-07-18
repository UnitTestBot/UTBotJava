package org.utbot.examples.stream

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withPathSelectorStepsLimit
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.AtLeast
import org.utbot.testing.Full
import org.utbot.testing.FullWithAssumptions
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException
import java.util.OptionalDouble
import java.util.OptionalLong
import java.util.stream.LongStream
import kotlin.streams.toList

// TODO failed Kotlin compilation (generics) JIRA:1332
@Tag("slow") // we do not really need to always use this test in CI because it is almost the same as BaseStreamExampleTest
class LongStreamExampleTest : UtValueTestCaseChecker(
    testClass = LongStreamExample::class,
    testCodeGeneration = true,
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun testReturningStreamAsParameterExample() {
        withoutConcrete {
            check(
                LongStreamExample::returningStreamAsParameterExample,
                eq(1),
                { s, r -> s != null && s.toList() == r!!.toList() },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testUseParameterStream() {
        check(
            LongStreamExample::useParameterStream,
            eq(2),
            { s, r -> s.toArray().isEmpty() && r == 0 },
            { s, r -> s.toArray().let {
                it.isNotEmpty() && r == it.size }
            },
            coverage = AtLeast(94)
        )
    }

    @Test
    fun testFilterExample() {
        check(
            LongStreamExample::filterExample,
            ignoreExecutionsNumber,
            { c, r -> null !in c && r == false },
            { c, r -> null in c && r == true },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapExample() {
        check(
            LongStreamExample::mapExample,
            ignoreExecutionsNumber,
            { c, r -> null in c && r.contentEquals(c.longs { it?.toLong()?.times(2) ?: 0L }) },
            { c: List<Short?>, r -> null !in c && r.contentEquals(c.longs { it?.toLong()?.times(2) ?: 0L }) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToObjExample() {
        check(
            LongStreamExample::mapToObjExample,
            ignoreExecutionsNumber,
            { c, r ->
                val intArrays = c.longs().map { it.let { i -> longArrayOf(i, i) } }.toTypedArray()

                null in c && intArrays.zip(r as Array<out Any>).all { it.first.contentEquals(it.second as LongArray?) }
            },
            { c: List<Short?>, r ->
                val intArrays = c.longs().map { it.let { i -> longArrayOf(i, i) } }.toTypedArray()

                null !in c && intArrays.zip(r as Array<out Any>).all { it.first.contentEquals(it.second as LongArray?) }
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToIntExample() {
        check(
            LongStreamExample::mapToIntExample,
            ignoreExecutionsNumber,
            { c, r ->
                val ints = c.longs().map { it.toInt() }.toIntArray()

                null in c && ints.contentEquals(r)
            },
            { c: List<Short?>, r ->
                val ints = c.longs().map { it.toInt() }.toIntArray()

                null !in c && ints.contentEquals(r)
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToDoubleExample() {
        check(
            LongStreamExample::mapToDoubleExample,
            ignoreExecutionsNumber,
            { c, r ->
                val doubles = c.longs().map { it.toDouble() / 2 }.toDoubleArray()

                null in c && doubles.contentEquals(r)
            },
            { c: List<Short?>, r ->
                val doubles = c.filterNotNull().map { it.toDouble() / 2 }.toDoubleArray()

                null !in c && doubles.contentEquals(r)
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testFlatMapExample() {
        check(
            LongStreamExample::flatMapExample,
            ignoreExecutionsNumber,
            { c, r ->
                val intLists = c.map {
                    (it?.toLong() ?: 0L).let { i -> listOf(i, i) }
                }

                r!!.contentEquals(intLists.flatten().toLongArray())
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testDistinctExample() {
        check(
            LongStreamExample::distinctExample,
            ignoreExecutionsNumber,
            { c, r ->
                val longs = c.longs()

                longs.contentEquals(longs.distinct().toLongArray()) && r == false
            },
            { c, r ->
                val longs = c.longs()

                !longs.contentEquals(longs.distinct().toLongArray()) && r == true
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    @Tag("slow")
    // TODO slow sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    fun testSortedExample() {
        check(
            LongStreamExample::sortedExample,
            ignoreExecutionsNumber,
            { c, r -> c.last() < c.first() && r!!.asSequence().isSorted() },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testPeekExample() {
        checkThisAndStaticsAfter(
            LongStreamExample::peekExample,
            ignoreExecutionsNumber,
            *streamConsumerStaticsMatchers,
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testLimitExample() {
        check(
            LongStreamExample::limitExample,
            ignoreExecutionsNumber,
            { c, r -> c.size <= 2 && c.longs().contentEquals(r) },
            { c, r -> c.size > 2 && c.take(2).longs().contentEquals(r) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSkipExample() {
        check(
            LongStreamExample::skipExample,
            ignoreExecutionsNumber,
            { c, r -> c.size > 2 && c.drop(2).longs().contentEquals(r) },
            { c, r -> c.size <= 2 && r!!.isEmpty() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testForEachExample() {
        checkThisAndStaticsAfter(
            LongStreamExample::forEachExample,
            ignoreExecutionsNumber,
            *streamConsumerStaticsMatchers,
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testToArrayExample() {
        check(
            LongStreamExample::toArrayExample,
            ignoreExecutionsNumber,
            { c, r -> c.longs().contentEquals(r) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testReduceExample() {
        check(
            LongStreamExample::reduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 42L },
            { c: List<Short?>, r -> c.isNotEmpty() && r == c.filterNotNull().sum() + 42L },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testOptionalReduceExample() {
        checkWithException(
            LongStreamExample::optionalReduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalLong.empty() },
            { c: List<Short?>, r ->
                c.isNotEmpty() && r.getOrThrow() == OptionalLong.of(
                    c.filterNotNull().sum().toLong()
                )
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSumExample() {
        check(
            LongStreamExample::sumExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0L },
            { c, r -> c.isNotEmpty() && c.filterNotNull().sum().toLong() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMinExample() {
        checkWithException(
            LongStreamExample::minExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalLong.empty() },
            { c, r -> c.isNotEmpty() && r.getOrThrow() == OptionalLong.of(c.mapNotNull { it.toLong() }.minOrNull()!!) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMaxExample() {
        checkWithException(
            LongStreamExample::maxExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalLong.empty() },
            { c, r -> c.isNotEmpty() && r.getOrThrow() == OptionalLong.of(c.mapNotNull { it.toLong() }.maxOrNull()!!) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testCountExample() {
        check(
            LongStreamExample::countExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0L },
            { c, r -> c.isNotEmpty() && c.size.toLong() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testAverageExample() {
        check(
            LongStreamExample::averageExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == OptionalDouble.empty() },
            { c, r -> c.isNotEmpty() && c.mapNotNull { it.toLong() }.average() == r!!.asDouble },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSummaryStatisticsExample() {
        withoutConcrete {
            check(
                LongStreamExample::summaryStatisticsExample,
                ignoreExecutionsNumber,
                { c, r ->
                    val sum = r!!.sum
                    val count = r.count
                    val min = r.min
                    val max = r.max

                    val allStatisticsAreCorrect = sum == 0L &&
                            count == 0L &&
                            min == Long.MAX_VALUE &&
                            max == Long.MIN_VALUE

                    c.isEmpty() && allStatisticsAreCorrect
                },
                { c, r ->
                    val sum = r!!.sum
                    val count = r.count
                    val min = r.min
                    val max = r.max

                    val longs = c.longs()

                    val allStatisticsAreCorrect = sum == longs.sum() &&
                            count == longs.size.toLong() &&
                            min == longs.minOrNull() &&
                            max == longs.maxOrNull()

                    c.isNotEmpty() && allStatisticsAreCorrect
                },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testAnyMatchExample() {
        // TODO exceeds even default step limit 3500 => too slow
        withPathSelectorStepsLimit(2000) {
            check(
                LongStreamExample::anyMatchExample,
                ignoreExecutionsNumber,
                { c, r -> c.isEmpty() && r == false },
                { c, r -> c.isNotEmpty() && c.longs().all { it == 0L } && r == false },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.first() != 0L && longs.last() == 0L && r == true
                },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.first() == 0L && longs.last() != 0L && r == true
                },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.none { it == 0L } && r == true
                },
                coverage = FullWithAssumptions(assumeCallsNumber = 2)
            )
        }
    }

    @Test
    fun testAllMatchExample() {
        // TODO exceeds even default step limit 3500 => too slow
        withPathSelectorStepsLimit(2000) {
            check(
                LongStreamExample::allMatchExample,
                ignoreExecutionsNumber,
                { c, r -> c.isEmpty() && r == true },
                { c, r -> c.isNotEmpty() && c.longs().all { it == 0L } && r == false },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.first() != 0L && longs.last() == 0L && r == false
                },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.first() == 0L && longs.last() != 0L && r == false
                },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.none { it == 0L } && r == true
                },
                coverage = FullWithAssumptions(assumeCallsNumber = 2)
            )
        }
    }

    @Test
    fun testNoneMatchExample() {
        // TODO exceeds even default step limit 3500 => too slow
        withPathSelectorStepsLimit(2000) {
            check(
                LongStreamExample::noneMatchExample,
                ignoreExecutionsNumber,
                { c, r -> c.isEmpty() && r == true },
                { c, r -> c.isNotEmpty() && c.longs().all { it == 0L } && r == true },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.first() != 0L && longs.last() == 0L && r == false
                },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.first() == 0L && longs.last() != 0L && r == false
                },
                { c, r ->
                    val longs = c.longs()

                    c.isNotEmpty() && longs.none { it == 0L } && r == false
                },
                coverage = FullWithAssumptions(assumeCallsNumber = 2)
            )
        }
    }

    @Test
    fun testFindFirstExample() {
        check(
            LongStreamExample::findFirstExample,
            eq(3),
            { c, r -> c.isEmpty() && r == OptionalLong.empty() },
            { c, r -> c.isNotEmpty() && r == OptionalLong.of(c.longs().first()) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testAsDoubleStreamExample() {
        check(
            LongStreamExample::asDoubleStreamExample,
            ignoreExecutionsNumber,
            { c, r -> c.longs().map { it.toDouble() }.toList() == r!!.toList() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testBoxedExample() {
        check(
            LongStreamExample::boxedExample,
            ignoreExecutionsNumber,
            { c, r -> c.longs().toList() == r!!.toList() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testIteratorExample() {
        // TODO exceeds even default step limit 3500 => too slow
        withPathSelectorStepsLimit(1000) {
            check(
                LongStreamExample::iteratorSumExample,
                ignoreExecutionsNumber,
                { c, r -> c.isEmpty() && r == 0L },
                { c: List<Short?>, r -> c.isNotEmpty() && c.longs().sum() == r },
                coverage = AtLeast(76)
            )
        }
    }

    @Test
    fun testStreamOfExample() {
        withoutConcrete {
            check(
                LongStreamExample::streamOfExample,
                ignoreExecutionsNumber,
                // NOTE: the order of the matchers is important because Stream could be used only once
                { c, r -> c.isNotEmpty() && c.contentEquals(r!!.toArray()) },
                { c, r -> c.isEmpty() && LongStream.empty().toArray().contentEquals(r!!.toArray()) },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testClosedStreamExample() {
        // TODO exceeds even default step limit 3500 => too slow
        withPathSelectorStepsLimit(500) {
            checkWithException(
                LongStreamExample::closedStreamExample,
                ignoreExecutionsNumber,
                { _, r -> r.isException<IllegalStateException>() },
                coverage = AtLeast(88)
            )
        }
    }

    @Test
    fun testGenerateExample() {
        check(
            LongStreamExample::generateExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(LongArray(10) { 42L }) },
            coverage = Full
        )
    }

    @Test
    fun testIterateExample() {
        check(
            LongStreamExample::iterateExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(LongArray(10) { i -> 42L + i }) },
            coverage = Full
        )
    }

    @Test
    fun testConcatExample() {
        check(
            LongStreamExample::concatExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(LongArray(10) { 42L } + LongArray(10) { i -> 42L + i }) },
            coverage = Full
        )
    }

    @Test
    fun testRangeExample() {
        check(
            LongStreamExample::rangeExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(LongArray(10) { it.toLong() }) },
            coverage = Full
        )
    }

    @Test
    fun testRangeClosedExample() {
        check(
            LongStreamExample::rangeClosedExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(LongArray(11) { it.toLong() }) },
            coverage = Full
        )
    }
}

private fun List<Short?>.longs(mapping: (Short?) -> Long = { it?.toLong() ?: 0L }): LongArray =
    map { mapping(it) }.toLongArray()
