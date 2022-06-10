package org.utbot.predictors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.examples.withRewardModelPath

class LinearStateRewardPredictorTest {
    @Test
    fun simpleTest() {
        withRewardModelPath("src/test/resources") {
            val pred = LinearStateRewardPredictor()

            val features = listOf(
                listOf(2.0, 3.0),
                listOf(2.0, 3.0)
            )

            assertEquals(listOf(6.0, 6.0), pred.predict(features))
        }
    }

    @Test
    fun wrongFormatTest() {
        withRewardModelPath("src/test/resources") {
            assertThrows<IllegalStateException> {
                LinearStateRewardPredictor("wrong_format_linear.txt")
            }
        }
    }
}