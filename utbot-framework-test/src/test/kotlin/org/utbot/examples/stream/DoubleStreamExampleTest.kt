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
import java.util.stream.DoubleStream
import kotlin.streams.toList

// TODO failed Kotlin compilation (generics) JIRA:1332
@Tag("slow") // we do not really need to always use this test in CI because it is almost the same as BaseStreamExampleTest
class DoubleStreamExampleTest : UtValueTestCaseChecker(
    testClass = DoubleStreamExample::class,
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun testReturningStreamAsParameterExample() {
        withoutConcrete {
            check(
                DoubleStreamExample::returningStreamAsParameterExample,
                eq(1),
                { s, r -> s != null && s.toList() == r!!.toList() },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testUseParameterStream() {
        check(
            DoubleStreamExample::useParameterStream,
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
            DoubleStreamExample::filterExample,
            ignoreExecutionsNumber,
            { c, r -> null !in c && r == false },
            { c, r -> null in c && r == true },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapExample() {
        check(
            DoubleStreamExample::mapExample,
            ignoreExecutionsNumber,
            { c, r -> null in c && r.contentEquals(c.doubles { it?.toDouble()?.times(2) ?: 0.0 }) },
            { c: List<Short?>, r -> null !in c && r.contentEquals(c.doubles { it?.toDouble()?.times(2) ?: 0.0 }) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToObjExample() {
        check(
            DoubleStreamExample::mapToObjExample,
            ignoreExecutionsNumber,
            { c, r ->
                val intArrays = c.doubles().map { it.let { i -> doubleArrayOf(i, i) } }.toTypedArray()

                null in c && intArrays.zip(r as Array<out Any>)
                    .all { it.first.contentEquals(it.second as DoubleArray?) }
            },
            { c: List<Short?>, r ->
                val intArrays = c.doubles().map { it.let { i -> doubleArrayOf(i, i) } }.toTypedArray()

                null !in c && intArrays.zip(r as Array<out Any>)
                    .all { it.first.contentEquals(it.second as DoubleArray?) }
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToIntExample() {
        check(
            DoubleStreamExample::mapToIntExample,
            ignoreExecutionsNumber,
            { c, r ->
                val ints = c.doubles().map { it.toInt() }.toIntArray()

                null in c && ints.contentEquals(r)
            },
            { c: List<Short?>, r ->
                val ints = c.doubles().map { it.toInt() }.toIntArray()

                null !in c && ints.contentEquals(r)
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToLongExample() {
        check(
            DoubleStreamExample::mapToLongExample,
            ignoreExecutionsNumber,
            { c, r ->
                val longs = c.doubles().map { it.toLong() }.toLongArray()

                null in c && longs.contentEquals(r)
            },
            { c: List<Short?>, r ->
                val longs = c.doubles().map { it.toLong() }.toLongArray()

                null !in c && longs.contentEquals(r)
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testFlatMapExample() {
        check(
            DoubleStreamExample::flatMapExample,
            ignoreExecutionsNumber,
            { c, r ->
                val intLists = c.mapNotNull {
                    it.toDouble().let { i -> listOf(i, i) }
                }

                r!!.contentEquals(intLists.flatten().toDoubleArray())
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testDistinctExample() {
        check(
            DoubleStreamExample::distinctExample,
            ignoreExecutionsNumber,
            { c, r ->
                val doubles = c.doubles()

                doubles.contentEquals(doubles.distinct().toDoubleArray()) && r == false
            },
            { c, r ->
                val doubles = c.doubles()

                !doubles.contentEquals(doubles.distinct().toDoubleArray()) && r == true
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    @Tag("slow")
    // TODO slow sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    fun testSortedExample() {
        check(
            DoubleStreamExample::sortedExample,
            ignoreExecutionsNumber,
            { c, r -> c.last() < c.first() && r!!.asSequence().isSorted() },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testPeekExample() {
        checkThisAndStaticsAfter(
            DoubleStreamExample::peekExample,
            ignoreExecutionsNumber,
            *streamConsumerStaticsMatchers,
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testLimitExample() {
        check(
            DoubleStreamExample::limitExample,
            ignoreExecutionsNumber,
            { c, r -> c.size <= 2 && c.doubles().contentEquals(r) },
            { c, r -> c.size > 2 && c.take(2).doubles().contentEquals(r) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSkipExample() {
        check(
            DoubleStreamExample::skipExample,
            ignoreExecutionsNumber,
            { c, r -> c.size > 2 && c.drop(2).doubles().contentEquals(r) },
            { c, r -> c.size <= 2 && r!!.isEmpty() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testForEachExample() {
        checkThisAndStaticsAfter(
            DoubleStreamExample::forEachExample,
            ignoreExecutionsNumber,
            *streamConsumerStaticsMatchers,
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testToArrayExample() {
        check(
            DoubleStreamExample::toArrayExample,
            ignoreExecutionsNumber,
            { c, r -> c.doubles().contentEquals(r) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testReduceExample() {
        check(
            DoubleStreamExample::reduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 42.0 },
            { c: List<Short?>, r -> c.isNotEmpty() && r == c.filterNotNull().sum() + 42.0 },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testOptionalReduceExample() {
        checkWithException(
            DoubleStreamExample::optionalReduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalDouble.empty() },
            { c: List<Short?>, r ->
                c.isNotEmpty() && r.getOrThrow() == OptionalDouble.of(
                    c.filterNotNull().sum().toDouble()
                )
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSumExample() {
        check(
            DoubleStreamExample::sumExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0.0 },
            { c, r -> c.isNotEmpty() && c.filterNotNull().sum().toDouble() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMinExample() {
        checkWithException(
            DoubleStreamExample::minExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalDouble.empty() },
            { c, r ->
                c.isNotEmpty() && r.getOrThrow() == OptionalDouble.of(c.mapNotNull { it.toDouble() }.minOrNull()!!)
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMaxExample() {
        checkWithException(
            DoubleStreamExample::maxExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalDouble.empty() },
            { c, r ->
                c.isNotEmpty() && r.getOrThrow() == OptionalDouble.of(c.mapNotNull { it.toDouble() }.maxOrNull()!!)
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testCountExample() {
        check(
            DoubleStreamExample::countExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0L },
            { c, r -> c.isNotEmpty() && c.size.toLong() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testAverageExample() {
        check(
            DoubleStreamExample::averageExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == OptionalDouble.empty() },
            { c, r -> c.isNotEmpty() && c.mapNotNull { it.toDouble() }.average() == r!!.asDouble },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSummaryStatisticsExample() {
        withoutConcrete {
            check(
                DoubleStreamExample::summaryStatisticsExample,
                ignoreExecutionsNumber,
                { c, r ->
                    val sum = r!!.sum
                    val count = r.count
                    val min = r.min
                    val max = r.max

                    val allStatisticsAreCorrect = sum == 0.0 &&
                            count == 0L &&
                            min == Double.POSITIVE_INFINITY &&
                            max == Double.NEGATIVE_INFINITY

                    c.isEmpty() && allStatisticsAreCorrect
                },
                { c, r ->
                    val sum = r!!.sum
                    val count = r.count
                    val min = r.min
                    val max = r.max

                    val doubles = c.doubles()

                    val allStatisticsAreCorrect = sum == doubles.sum() &&
                            count == doubles.size.toLong() &&
                            min == doubles.minOrNull() &&
                            max == doubles.maxOrNull()

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
                DoubleStreamExample::anyMatchExample,
                ignoreExecutionsNumber,
                { c, r -> c.isEmpty() && r == false },
                { c, r -> c.isNotEmpty() && c.doubles().all { it == 0.0 } && r == false },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.first() != 0.0 && doubles.last() == 0.0 && r == true
                },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.first() == 0.0 && doubles.last() != 0.0 && r == true
                },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.none { it == 0.0 } && r == true
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
                DoubleStreamExample::allMatchExample,
                ignoreExecutionsNumber,
                { c, r -> c.isEmpty() && r == true },
                { c, r -> c.isNotEmpty() && c.doubles().all { it == 0.0 } && r == false },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.first() != 0.0 && doubles.last() == 0.0 && r == false
                },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.first() == 0.0 && doubles.last() != 0.0 && r == false
                },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.none { it == 0.0 } && r == true
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
                DoubleStreamExample::noneMatchExample,
                ignoreExecutionsNumber,
                { c, r -> c.isEmpty() && r == true },
                { c, r -> c.isNotEmpty() && c.doubles().all { it == 0.0 } && r == true },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.first() != 0.0 && doubles.last() == 0.0 && r == false
                },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.first() == 0.0 && doubles.last() != 0.0 && r == false
                },
                { c, r ->
                    val doubles = c.doubles()

                    c.isNotEmpty() && doubles.none { it == 0.0 } && r == false
                },
                coverage = FullWithAssumptions(assumeCallsNumber = 2)
            )
        }
    }

    @Test
    fun testFindFirstExample() {
        check(
            DoubleStreamExample::findFirstExample,
            eq(3),
            { c, r -> c.isEmpty() && r == OptionalDouble.empty() },
            { c, r -> c.isNotEmpty() && r == OptionalDouble.of(c.doubles().first()) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testBoxedExample() {
        check(
            DoubleStreamExample::boxedExample,
            ignoreExecutionsNumber,
            { c, r -> c.doubles().toList() == r!!.toList() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testIteratorExample() {
        // TODO exceeds even default step limit 3500 => too slow
        withPathSelectorStepsLimit(1000) {
            check(
                DoubleStreamExample::iteratorSumExample,
                ignoreExecutionsNumber,
                { c, r -> c.isEmpty() && r == 0.0 },
                { c: List<Short?>, r -> c.isNotEmpty() && c.doubles().sum() == r },
                coverage = AtLeast(76)
            )
        }
    }

    @Test
    fun testStreamOfExample() {
        withoutConcrete {
            check(
                DoubleStreamExample::streamOfExample,
                ignoreExecutionsNumber,
                // NOTE: the order of the matchers is important because Stream could be used only once
                { c, r -> c.isNotEmpty() && c.contentEquals(r!!.toArray()) },
                { c, r -> c.isEmpty() && DoubleStream.empty().toArray().contentEquals(r!!.toArray()) },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testClosedStreamExample() {
        // TODO exceeds even default step limit 3500 => too slow
        withPathSelectorStepsLimit(500) {
            checkWithException(
                DoubleStreamExample::closedStreamExample,
                ignoreExecutionsNumber,
                { _, r -> r.isException<IllegalStateException>() },
                coverage = AtLeast(88)
            )
        }
    }

    @Test
    fun testGenerateExample() {
        check(
            DoubleStreamExample::generateExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(DoubleArray(10) { 42.0 }) },
            coverage = Full
        )
    }

    @Test
    fun testIterateExample() {
        check(
            DoubleStreamExample::iterateExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(DoubleArray(10) { i -> 42.0 + i }) },
            coverage = Full
        )
    }

    @Test
    fun testConcatExample() {
        check(
            DoubleStreamExample::concatExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(DoubleArray(10) { 42.0 } + DoubleArray(10) { i -> 42.0 + i }) },
            coverage = Full
        )
    }
}

private fun List<Short?>.doubles(mapping: (Short?) -> Double = { it?.toDouble() ?: 0.0 }): DoubleArray =
    map { mapping(it) }.toDoubleArray()
