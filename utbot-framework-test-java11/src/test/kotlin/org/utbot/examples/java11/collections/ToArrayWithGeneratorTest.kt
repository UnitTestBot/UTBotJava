package org.utbot.examples.java11.collections

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException

class ToArrayWithGeneratorTest : UtValueTestCaseChecker(
    testClass = ToArrayWithGenerator::class
) {
    @Test
    fun testCheckSetSize() {
        check(
            ToArrayWithGenerator::checkSetSize,
            ignoreExecutionsNumber,
            { size, result -> size < 0 && result == false },
            { size, result -> size >= 0 && result == true }
        )
    }

    @Test
    fun testCheckSetSizeArrayStoreException() {
        checkWithException(
            ToArrayWithGenerator::checkSetSizeArrayStoreException,
            eq(3),
            { size, result -> size < 0 && result.isSuccess && result.getOrNull() == false },
            { size, result -> size == 0 && result.isSuccess && result.getOrNull() == true },
            { size, result -> size > 0 && result.isException<ArrayStoreException>() }
        )
    }

    @Test
    fun testCheckListSize() {
        check(
            ToArrayWithGenerator::checkListSize,
            ignoreExecutionsNumber,
            { size, result -> size < 0 && result == false },
            { size, result -> size >= 0 && result == true }
        )
    }

    @Test
    fun testCheckMapKeysSize() {
        check(
            ToArrayWithGenerator::checkMapKeysSize,
            ignoreExecutionsNumber,
            { size, result -> size < 0 && result == false },
            { size, result -> size >= 0 && result == true }
        )
    }

    @Test
    fun testCheckMapValuesSize() {
        check(
            ToArrayWithGenerator::checkMapValuesSize,
            ignoreExecutionsNumber,
            { size, result -> size < 0 && result == false },
            { size, result -> size >= 0 && result == true }
        )
    }

    @Test
    fun testGetMapEntrySetArrayStoreException() {
        checkWithException(
            ToArrayWithGenerator::getMapEntrySetArrayStoreException,
            eq(1),
            { result -> result.isFailure && result.isException<ArrayStoreException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetMapEntrySetSize() {
        check(
            ToArrayWithGenerator::getMapEntrySetSize,
            eq(1),
            { result -> result == 2 }
        )
    }
}
