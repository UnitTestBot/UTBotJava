package org.utbot.summary.clustering

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import org.utbot.framework.plugin.api.Step
import java.lang.IllegalArgumentException

internal class ExecutionMetricTest {
    @Test
    fun computeWithTwoEmptySteps() {
        val executionMetric = ExecutionMetric()
        val object1 = listOf<Step>()
        val object2 = listOf<Step>()


        val exception = Assertions.assertThrows(IllegalArgumentException::class.java) {
            executionMetric.compute(object1 = object1, object2 = object2)
        }

        Assertions.assertEquals(
            "Two paths can not be compared: path1 is empty!",
            exception.message
        )
    }
}