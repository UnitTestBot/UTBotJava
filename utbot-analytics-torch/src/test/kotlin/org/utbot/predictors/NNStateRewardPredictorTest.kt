package org.utbot.predictors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.analytics.StateRewardPredictor
import org.utbot.testcheckers.withModelPath
import kotlin.system.measureNanoTime

class NNStateRewardPredictorTest {
    @Test
    @Disabled("Just to see the performance of predictors")
    fun simpleTest() {
        withModelPath("src/test/resources") {
            val pred = StateRewardPredictorTorch()

            val features = listOf(0.0, 0.0)

            assertEquals(5.0, pred.predict(features))
        }
    }

    @Disabled("Just to see the performance of predictors")
    @Test
    fun performanceTest() {
        val features = (1..13).map { 1.0 }.toList()
        withModelPath("models") {
            val averageTime = calcAverageTimeForModelPredict(::StateRewardPredictorTorch, 100, features)
            println(averageTime)
        }
    }

    private fun calcAverageTimeForModelPredict(
        model: () -> StateRewardPredictor,
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
}