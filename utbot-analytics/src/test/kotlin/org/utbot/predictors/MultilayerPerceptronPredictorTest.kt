package org.utbot.predictors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.analytics.MLPredictor
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import org.utbot.testcheckers.withPathSelectorType
import org.utbot.testcheckers.withModelPath
import kotlin.system.measureNanoTime

class MultilayerPerceptronPredictorTest {
    @Test
    fun simpleTest() {
        withModelPath("src/test/resources") {
            val pred = MultilayerPerceptronPredictor()

            val features = listOf(0.0, 0.0)

            assertEquals(5.0, pred.predict(features))
        }
    }

    @Disabled("Just to see the performance of predictors")
    @Test
    fun performanceTest() {
        val features = (1..13).map { 1.0 }.toList()
        withModelPath("models\\test\\0") {
            val averageTime = calcAverageTimeForModelPredict(::MultilayerPerceptronPredictor, 100, features)
            println(averageTime)
        }
    }

    internal fun calcAverageTimeForModelPredict(
        model: () -> MLPredictor,
        iterations: Int,
        features: List<Double>
    ): Double {
        val pred = model()

        (1..iterations).map {
            pred.predict(features)
        }

        return (1..iterations)
            .map { measureNanoTime { pred.predict(features) } }
            .average()
    }

    @Test
    fun corruptedModelFileTest() {
        withModelPath("src/test/resources") {
            withPathSelectorType(PathSelectorType.ML_SELECTOR) {
                MultilayerPerceptronPredictor(modelPath = "corrupted_nn.json")
                assertEquals(PathSelectorType.INHERITORS_SELECTOR, UtSettings.pathSelectorType)
            }
        }
    }

    @Test
    fun emptyModelFileTest() {
        withModelPath("src/test/resources") {
            withPathSelectorType(PathSelectorType.ML_SELECTOR) {
                MultilayerPerceptronPredictor(modelPath = "empty_nn.json")
                assertEquals(PathSelectorType.INHERITORS_SELECTOR, UtSettings.pathSelectorType)
            }
        }
    }

    @Test
    fun corruptedScalerTest() {
        withModelPath("src/test/resources") {
            withPathSelectorType(PathSelectorType.ML_SELECTOR) {
                MultilayerPerceptronPredictor(scalerPath = "corrupted_scaler.txt")
                assertEquals(PathSelectorType.INHERITORS_SELECTOR, UtSettings.pathSelectorType)
            }
        }
    }
}