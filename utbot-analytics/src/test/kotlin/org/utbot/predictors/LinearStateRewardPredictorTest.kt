package org.utbot.predictors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import org.utbot.testcheckers.withPathSelectorType
import org.utbot.testcheckers.withRewardModelPath

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
            withPathSelectorType(PathSelectorType.NN_REWARD_GUIDED_SELECTOR) {
                LinearStateRewardPredictor("wrong_format_linear.txt")
                assertEquals(PathSelectorType.INHERITORS_SELECTOR, UtSettings.pathSelectorType)
            }
        }
    }

    @Test
    fun simpleTestNotBatch() {
        withRewardModelPath("src/test/resources") {
            val pred = LinearStateRewardPredictor()

            val features = listOf(2.0, 3.0)

            assertEquals(6.0, pred.predict(features))
        }
    }
}