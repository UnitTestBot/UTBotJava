package org.utbot.predictors

import org.utbot.framework.UtSettings
import java.nio.file.Paths
import ai.djl.*
import ai.djl.inference.*;
import ai.djl.ndarray.*;
import ai.djl.translate.*;
import java.io.Closeable

class NNStateRewardPredictorTorch : NNStateRewardPredictor, Closeable {
    val model: ai.djl.Model = ai.djl.Model.newInstance("model")
    init {
        model.load(Paths.get(UtSettings.rewardModelPath, "model.pt1"))
    }
    val predictor: Predictor<List<Float>, Float> = model.newPredictor(object : Translator<List<Float>, Float> {
        override fun processInput(ctx: TranslatorContext, input: List<Float>): NDList {
            val manager: NDManager = ctx.getNDManager()
            val array: NDArray = manager.create(input.toFloatArray())
            return NDList(array)
        }

        override fun processOutput(ctx: TranslatorContext, list: NDList): Float {
            val tmp: NDArray = list.get(0)
            return tmp.getFloat()
        }
    })

    override fun predict(input: List<Double>): Double {
        val reward: Float = predictor.predict(input.map { it.toFloat() }.toList())
        return reward.toDouble()
    }

    override fun close() {
        predictor.close()
    }
}