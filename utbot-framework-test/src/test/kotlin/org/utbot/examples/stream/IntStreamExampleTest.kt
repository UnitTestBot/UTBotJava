package org.utbot.examples.stream

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.examples.AtLeast
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.Full
import org.utbot.examples.FullWithAssumptions
import org.utbot.examples.eq
import org.utbot.examples.ignoreExecutionsNumber
import org.utbot.examples.isException
import org.utbot.examples.withoutConcrete
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.stream.IntStream
import kotlin.streams.toList

// TODO failed Kotlin compilation (generics) JIRA:1332
//@Tag("slow") // we do not really need to always use this test in CI because it is almost the same as BaseStreamExampleTest
class IntStreamExampleTest : UtValueTestCaseChecker(
    testClass = IntStreamExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testReturningStreamExample() {
        check(
            IntStreamExample::returningStreamExample,
            ignoreExecutionsNumber,
            // NOTE: the order of the matchers is important because Stream could be used only once
            { c, r -> c.isNotEmpty() && c.ints().contentEquals(r!!.toArray()) },
            { c, r -> c.isEmpty() && r!!.count() == 0L },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testReturningStreamAsParameterExample() {
        withoutConcrete {
            check(
                IntStreamExample::returningStreamAsParameterExample,
                eq(1),
                { s, r -> s != null && s.toList() == r!!.toList() },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testFilterExample() {
        check(
            IntStreamExample::filterExample,
            ignoreExecutionsNumber,
            { c, r -> null !in c && r == false },
            { c, r -> null in c && r == true },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapExample() {
        check(
            IntStreamExample::mapExample,
            ignoreExecutionsNumber,
            { c, r -> null in c && r.contentEquals(c.ints { it?.toInt()?.times(2) ?: 0 }) },
            { c: List<Short?>, r -> null !in c && r.contentEquals(c.ints { it?.toInt()?.times(2) ?: 0 }) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToObjExample() {
        check(
            IntStreamExample::mapToObjExample,
            ignoreExecutionsNumber,
            { c, r ->
                val intArrays = c.ints().map { it.let { i -> intArrayOf(i, i) } }.toTypedArray()

                null in c && intArrays.zip(r as Array<out Any>).all { it.first.contentEquals(it.second as IntArray?) }
            },
            { c: List<Short?>, r ->
                val intArrays = c.ints().map { it.let { i -> intArrayOf(i, i) } }.toTypedArray()

                null !in c && intArrays.zip(r as Array<out Any>).all { it.first.contentEquals(it.second as IntArray?) }
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToLongExample() {
        check(
            IntStreamExample::mapToLongExample,
            ignoreExecutionsNumber,
            { c, r ->
                val longs = c.ints().map { it.toLong() * 2 }.toLongArray()

                null in c && longs.contentEquals(r)
            },
            { c: List<Short?>, r ->
                val longs = c.ints().map { it.toLong() * 2 }.toLongArray()

                null !in c && longs.contentEquals(r)
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMapToDoubleExample() {
        check(
            IntStreamExample::mapToDoubleExample,
            ignoreExecutionsNumber,
            { c, r ->
                val doubles = c.ints().map { it.toDouble() / 2 }.toDoubleArray()

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
            IntStreamExample::flatMapExample,
            ignoreExecutionsNumber,
            { c, r ->
                val intLists = c.mapNotNull {
                    it.toInt().let { i -> listOf(i, i) }
                }

                r!!.contentEquals(intLists.flatten().toIntArray())
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testDistinctExample() {
        check(
            IntStreamExample::distinctExample,
            ignoreExecutionsNumber,
            { c, r ->
                val ints = c.ints()

                ints.contentEquals(ints.distinct().toIntArray()) && r == false
            },
            { c, r ->
                val ints = c.ints()

                !ints.contentEquals(ints.distinct().toIntArray()) && r == true
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    @Tag("slow")
    // TODO slow sorting https://github.com/UnitTestBot/UTBotJava/issues/188
    fun testSortedExample() {
        check(
            IntStreamExample::sortedExample,
            ignoreExecutionsNumber,
            { c, r -> c.last() < c.first() && r!!.asSequence().isSorted() },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testPeekExample() {
        checkThisAndStaticsAfter(
            IntStreamExample::peekExample,
            ignoreExecutionsNumber,
            *streamConsumerStaticsMatchers,
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testLimitExample() {
        check(
            IntStreamExample::limitExample,
            ignoreExecutionsNumber,
            { c, r -> c.size <= 5 && c.ints().contentEquals(r) },
            { c, r -> c.size > 5 && c.take(5).ints().contentEquals(r) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSkipExample() {
        check(
            IntStreamExample::skipExample,
            ignoreExecutionsNumber,
            { c, r -> c.size > 5 && c.drop(5).ints().contentEquals(r) },
            { c, r -> c.size <= 5 && r!!.isEmpty() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testForEachExample() {
        checkThisAndStaticsAfter(
            IntStreamExample::forEachExample,
            ignoreExecutionsNumber,
            *streamConsumerStaticsMatchers,
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testToArrayExample() {
        check(
            IntStreamExample::toArrayExample,
            ignoreExecutionsNumber,
            { c, r -> c.ints().contentEquals(r) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testReduceExample() {
        check(
            IntStreamExample::reduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 42 },
            { c: List<Short?>, r -> c.isNotEmpty() && r == c.filterNotNull().sum() + 42 },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testOptionalReduceExample() {
        checkWithException(
            IntStreamExample::optionalReduceExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalInt.empty() },
            { c: List<Short?>, r -> c.isNotEmpty() && r.getOrThrow() == OptionalInt.of(c.filterNotNull().sum()) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSumExample() {
        check(
            IntStreamExample::sumExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0 },
            { c, r -> c.isNotEmpty() && c.filterNotNull().sum() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMinExample() {
        checkWithException(
            IntStreamExample::minExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalInt.empty() },
            { c, r -> c.isNotEmpty() && r.getOrThrow() == OptionalInt.of(c.mapNotNull { it.toInt() }.minOrNull()!!) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testMaxExample() {
        checkWithException(
            IntStreamExample::maxExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r.getOrThrow() == OptionalInt.empty() },
            { c, r -> c.isNotEmpty() && r.getOrThrow() == OptionalInt.of(c.mapNotNull { it.toInt() }.maxOrNull()!!) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testCountExample() {
        check(
            IntStreamExample::countExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0L },
            { c, r -> c.isNotEmpty() && c.size.toLong() == r },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testAverageExample() {
        check(
            IntStreamExample::averageExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == OptionalDouble.empty() },
            { c, r -> c.isNotEmpty() && c.mapNotNull { it.toInt() }.average() == r!!.asDouble },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testSummaryStatisticsExample() {
        withoutConcrete {
            check(
                IntStreamExample::summaryStatisticsExample,
                ignoreExecutionsNumber,
                { c, r ->
                    val sum = r!!.sum
                    val count = r.count
                    val min = r.min
                    val max = r.max

                    val allStatisticsAreCorrect = sum == 0L &&
                            count == 0L &&
                            min == Int.MAX_VALUE &&
                            max == Int.MIN_VALUE

                    c.isEmpty() && allStatisticsAreCorrect
                },
                { c, r ->
                    val sum = r!!.sum
                    val count = r.count
                    val min = r.min
                    val max = r.max

                    val ints = c.ints()

                    val allStatisticsAreCorrect = sum == ints.sum().toLong() &&
                            count == ints.size.toLong() &&
                            min == ints.minOrNull() &&
                            max == ints.maxOrNull()

                    c.isNotEmpty() && allStatisticsAreCorrect
                },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testAnyMatchExample() {
        check(
            IntStreamExample::anyMatchExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == false },
            { c, r -> c.isNotEmpty() && c.ints().all { it == 0 } && r == false },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.first() != 0 && ints.last() == 0 && r == true
            },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.first() == 0 && ints.last() != 0 && r == true
            },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.none { it == 0 } && r == true
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testAllMatchExample() {
        check(
            IntStreamExample::allMatchExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == true },
            { c, r -> c.isNotEmpty() && c.ints().all { it == 0 } && r == false },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.first() != 0 && ints.last() == 0 && r == false
            },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.first() == 0 && ints.last() != 0 && r == false
            },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.none { it == 0 } && r == true
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testNoneMatchExample() {
        check(
            IntStreamExample::noneMatchExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == true },
            { c, r -> c.isNotEmpty() && c.ints().all { it == 0 } && r == true },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.first() != 0 && ints.last() == 0 && r == false
            },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.first() == 0 && ints.last() != 0 && r == false
            },
            { c, r ->
                val ints = c.ints()

                c.isNotEmpty() && ints.none { it == 0 } && r == false
            },
            coverage = FullWithAssumptions(assumeCallsNumber = 2)
        )
    }

    @Test
    fun testFindFirstExample() {
        check(
            IntStreamExample::findFirstExample,
            eq(3),
            { c, r -> c.isEmpty() && r == OptionalInt.empty() },
            { c, r -> c.isNotEmpty() && r == OptionalInt.of(c.ints().first()) },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testAsLongStreamExample() {
        check(
            IntStreamExample::asLongStreamExample,
            ignoreExecutionsNumber,
            { c, r -> c.ints().map { it.toLong() }.toList() == r!!.toList() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testAsDoubleStreamExample() {
        check(
            IntStreamExample::asDoubleStreamExample,
            ignoreExecutionsNumber,
            { c, r -> c.ints().map { it.toDouble() }.toList() == r!!.toList() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testBoxedExample() {
        check(
            IntStreamExample::boxedExample,
            ignoreExecutionsNumber,
            { c, r -> c.ints().toList() == r!!.toList() },
            coverage = FullWithAssumptions(assumeCallsNumber = 1)
        )
    }

    @Test
    fun testIteratorExample() {
        check(
            IntStreamExample::iteratorSumExample,
            ignoreExecutionsNumber,
            { c, r -> c.isEmpty() && r == 0 },
            { c: List<Short?>, r -> c.isNotEmpty() && c.ints().sum() == r },
            coverage = AtLeast(76)
        )
    }

    @Test
    fun testStreamOfExample() {
        withoutConcrete {
            check(
                IntStreamExample::streamOfExample,
                ignoreExecutionsNumber,
                // NOTE: the order of the matchers is important because Stream could be used only once
                { c, r -> c.isNotEmpty() && c.contentEquals(r!!.toArray()) },
                { c, r -> c.isEmpty() && IntStream.empty().toArray().contentEquals(r!!.toArray()) },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testClosedStreamExample() {
        checkWithException(
            IntStreamExample::closedStreamExample,
            ignoreExecutionsNumber,
            { _, r -> r.isException<IllegalStateException>() },
            coverage = AtLeast(88)
        )
    }

    @Test
    fun testGenerateExample() {
        check(
            IntStreamExample::generateExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(IntArray(10) { 42 }) },
            coverage = Full
        )
    }

    @Test
    fun testIterateExample() {
        check(
            IntStreamExample::iterateExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(IntArray(10) { i -> 42 + i }) },
            coverage = Full
        )
    }

    @Test
    fun testConcatExample() {
        check(
            IntStreamExample::concatExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(IntArray(10) { 42 } + IntArray(10) { i -> 42 + i }) },
            coverage = Full
        )
    }

    @Test
    fun testRangeExample() {
        check(
            IntStreamExample::rangeExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(IntArray(10) { it }) },
            coverage = Full
        )
    }

    @Test
    fun testRangeClosedExample() {
        check(
            IntStreamExample::rangeClosedExample,
            ignoreExecutionsNumber,
            { r -> r!!.contentEquals(IntArray(11) { it }) },
            coverage = Full
        )
    }
}

private fun List<Short?>.ints(mapping: (Short?) -> Int = { it?.toInt() ?: 0 }): IntArray =
    map { mapping(it) }.toIntArray()
