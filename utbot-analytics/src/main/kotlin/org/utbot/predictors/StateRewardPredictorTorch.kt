package org.utbot.predictors

import ai.djl.Model
import ai.djl.inference.Predictor
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDList
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import org.utbot.analytics.StateRewardPredictor
import org.utbot.framework.UtSettings
import java.io.Closeable
import java.nio.file.Paths

class StateRewardPredictorTorch : StateRewardPredictor, Closeable {
    val model: Model = Model.newInstance("model")

    init {
        model.load(Paths.get(UtSettings.rewardModelPath, "model.pt1"))
    }

    private val predictor: Predictor<List<Float>, Float> = model.newPredictor(object : Translator<List<Float>, Float> {
        override fun processInput(ctx: TranslatorContext, input: List<Float>): NDList {
            val array: NDArray = ctx.ndManager.create(input.toFloatArray())
            return NDList(array)
        }

        override fun processOutput(ctx: TranslatorContext, list: NDList): Float = list[0].getFloat()
    })

    override fun predict(input: List<Double>): Double {
        val reward: Float = predictor.predict(input.map { it.toFloat() }.toList())
        return reward.toDouble()
    }

    override fun close() {
        predictor.close()
    }
}