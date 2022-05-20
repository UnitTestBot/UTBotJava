package org.utbot.analytics.examples.inner

import org.utbot.analytics.examples.SummaryTestCaseGeneratorTest
import org.utbot.examples.inner.NestedCalls
import org.junit.jupiter.api.Test

class SummaryNestedCallsTest : SummaryTestCaseGeneratorTest(
    NestedCalls::class,
) {

    val summaryNestedCall1 = "Test calls NestedCalls\$ExceptionExamples::initAnArray,\n" +
            "    there it catches exception:\n" +
            "        {@code NegativeArraySizeException e}"
    val summaryNestedCall2 = "Test calls NestedCalls\$ExceptionExamples::initAnArray,\n" +
            "    there it catches exception:\n" +
            "        {@code IndexOutOfBoundsException e}"
    val summaryNestedCall3 = "Test calls NestedCalls\$ExceptionExamples::initAnArray,\n" +
            "    there it catches exception:\n" +
            "        {@code IndexOutOfBoundsException e}"
    val summaryNestedCall4 = "<pre>\n" +
            "Test calls NestedCalls\$ExceptionExamples::initAnArray,\n" +
            "    there it returns from: {@code return a[n - 1] + a[n - 2];}"

    @Test
    fun testInvokeExample() {
        checkOneArgument(
            NestedCalls::callInitExamples,
            summaryKeys = listOf(
                summaryNestedCall1,
                summaryNestedCall2,
                summaryNestedCall3,
                summaryNestedCall4,
            ),
            displayNames = listOf(
                "Catch (NegativeArraySizeException e) -> return -2",
                "Catch (IndexOutOfBoundsException e) -> return -3",
                "Catch (IndexOutOfBoundsException e) -> return -3",
                "initAnArray -> return a[n - 1] + a[n - 2]"
            )
        )
    }
}