package org.utbot.predictors

import org.utbot.examples.withRewardModelPath
import kotlin.system.measureNanoTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class NNStateRewardPredictorTest {
    @Test
    fun simpleTest() {
        withRewardModelPath("src/test/resources") {
            val pred = NNStateRewardPredictorSmile()

            val features = listOf(
                0.0, 0.0
            )

            assertEquals(5.0, pred.predict(features))
        }
    }

    @Disabled("Just to see the performance of predictors")
    @Test
    fun performanceTest() {
        val features = (1..13).map { 1.0 }.toList()
        withRewardModelPath("models\\test\\0") {
            val pred1 = NNStateRewardPredictorSmile()

            (1..100).map {
                pred1.predict(features)
            }

            println((1..100).map {
                measureNanoTime { pred1.predict(features) }
            }.sum().toDouble() / 100)
        }


        withRewardModelPath("models") {
            val pred2 = NNStateRewardPredictorTorch()

            (1..100).map {
                pred2.predict(features)
            }

            println((1..100).map {
                measureNanoTime { pred2.predict(features) }
            }.sum().toDouble() / 100)
            pred2.close()
        }
    }
}