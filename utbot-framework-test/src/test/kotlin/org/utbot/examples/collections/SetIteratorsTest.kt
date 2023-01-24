package org.utbot.examples.collections

import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.CodeGeneration
import org.utbot.testing.FullWithAssumptions
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.between
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException

// TODO failed Kotlin compilation SAT-1332
class SetIteratorsTest : UtValueTestCaseChecker(
    testClass = SetIterators::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testReturnIterator() {
        withoutConcrete { // We need to check that a real class is returned but not `Ut` one
            check(
                SetIterators::returnIterator,
                ignoreExecutionsNumber,
                { s, r -> s.isEmpty() && r!!.asSequence().toSet().isEmpty() },
                { s, r -> s.isNotEmpty() && r!!.asSequence().toSet() == s },
                coverage = FullWithAssumptions(assumeCallsNumber = 1)
            )
        }
    }

    @Test
    fun testIteratorHasNext() {
        check(
            SetIterators::iteratorHasNext,
            between(3..4),
            { set, _ -> set == null },
            { set, result -> set.isEmpty() && result == 0 },
            { set, result -> set.isNotEmpty() && result == set.size },
        )
    }

    @Test
    fun testIteratorNext() {
        checkWithException(
            SetIterators::iteratorNext,
            between(3..4),
            { set, result -> set == null && result.isException<NullPointerException>() },
            { set, result -> set != null && set.isEmpty() && result.isException<NoSuchElementException>() },
            // test should work as long as default class for set is LinkedHashSet
            { set, result -> set != null && set.isNotEmpty() && result.getOrNull() == set.first() },
        )
    }

    @Test
    fun testIteratorRemove() {
        checkWithException(
            SetIterators::iteratorRemove,
            between(3..4),
            { set, result -> set == null && result.isException<NullPointerException>() },
            { set, result -> set.isEmpty() && result.isException<NoSuchElementException>() },
            // test should work as long as default class for set is LinkedHashSet
            { set, result ->
                val firstElement  = set.first()
                val resultSet = result.getOrNull()!!
                val resultDoesntContainFirstElement = resultSet.size == set.size - 1 && firstElement !in resultSet
                set.isNotEmpty() && set.containsAll(resultSet) && resultDoesntContainFirstElement
            },
        )
    }

    @Test
    fun testIteratorRemoveOnIndex() {
        checkWithException(
            SetIterators::iteratorRemoveOnIndex,
            ge(5),
            { _, i, result -> i == 0 && result.isSuccess && result.getOrNull() == null },
            { set, _, result -> set == null && result.isException<NullPointerException>() },
            { set, i, result -> set != null && i < 0 && result.isException<IllegalStateException>() },
            { set, i, result -> i > set.size && result.isException<NoSuchElementException>() },
            // test should work as long as default class for set is LinkedHashSet
            { set, i, result ->
                val ithElement = set.toList()[i - 1]
                val resultSet = result.getOrNull()!!
                val iInIndexRange = i in 0..set.size
                val resultDoesntContainIthElement = resultSet.size == set.size - 1 && ithElement !in resultSet
                iInIndexRange && set.containsAll(resultSet) && resultDoesntContainIthElement
            },
        )
    }

    @Test
    fun testIterateForEach() {
        check(
            SetIterators::iterateForEach,
            ignoreExecutionsNumber,
            { set, _ -> set == null },
            { set, _ -> set != null && null in set },
            { set, result -> set != null && result == set.sum() },
        )
    }


    @Test
    fun testIterateWithIterator() {
        check(
            SetIterators::iterateWithIterator,
            ignoreExecutionsNumber,
            { set, _ -> set == null },
            { set, _ -> set != null && null in set },
            { set, result -> set != null && result == set.sum() },
        )
    }
}