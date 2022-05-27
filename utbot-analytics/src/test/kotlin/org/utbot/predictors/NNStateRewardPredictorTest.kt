package org.utbot.predictors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.examples.withRewardModelPath
import kotlin.system.measureNanoTime

class NNStateRewardPredictorTest {
    @Test
    fun simpleTest() {
        withRewardModelPath("src/test/resources") {
            val pred = NNStateRewardPredictorSmile()

            val features = listOf(0.0, 0.0)

            assertEquals(5.0, pred.predict(features))
        }
    }

    @Disabled("Just to see the performance of predictors")
    @Test
    fun performanceTest() {
        val features = (1..13).map { 1.0 }.toList()
        withRewardModelPath("models\\test\\0") {
            val averageTime = calcAverageTimeForModelPredict(::NNStateRewardPredictorSmile, 100, features)
            println(averageTime)
        }


        withRewardModelPath("models") {
            val averageTime = calcAverageTimeForModelPredict(::NNStateRewardPredictorTorch, 100, features)
            println(averageTime)
        }
    }

    private fun calcAverageTimeForModelPredict(
        model: () -> NNStateRewardPredictor,
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