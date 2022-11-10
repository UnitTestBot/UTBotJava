package org.utbot.examples.taint

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.UtValueTestCaseChecker

internal class CollectionExamplesTest : UtValueTestCaseChecker(
    testClass = CollectionsExamples::class
) {
    @Test
    fun testSinkWithList() {
        check(
            CollectionsExamples::sinkWithList,
            eq(-1),
        )
    }

    @Test
    fun testPassThroughExample() {
        check(
            CollectionsExamples::passThroughExample,
            eq(-1),
        )
    }
}