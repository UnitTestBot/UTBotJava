package org.utbot.predictors

import org.utbot.examples.withRewardModelPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}