package org.utbot.predictors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import org.utbot.testcheckers.withPathSelectorType
import org.utbot.testcheckers.withModelPath

class LinearRegressionPredictorTest {
    @Test
    fun simpleTest() {
        withModelPath("src/test/resources") {
            val pred = LinearRegressionPredictor()

            val features = listOf(
                listOf(2.0, 3.0),
                listOf(2.0, 3.0)
            )

            assertEquals(listOf(6.0, 6.0), pred.predict(features))
        }
    }

    @Test
    fun wrongFormatTest() {
        withModelPath("src/test/resources") {
            withPathSelectorType(PathSelectorType.ML_SELECTOR) {
                LinearRegressionPredictor("wrong_format_linear.txt")
                assertEquals(PathSelectorType.INHERITORS_SELECTOR, UtSettings.pathSelectorType)
            }
        }
    }

    @Test
    fun simpleTestNotBatch() {
        withModelPath("src/test/resources") {
            val pred = LinearRegressionPredictor()

            val features = listOf(2.0, 3.0)

            assertEquals(6.0, pred.predict(features))
        }
    }
}