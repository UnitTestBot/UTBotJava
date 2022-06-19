package org.utbot.utils.layers

import org.utbot.utils.MatrixUtil
import java.io.FileNotFoundException


class SimpleDecoder {

    lateinit var weight0: Array<DoubleArray>
    lateinit var weight1: Array<DoubleArray>
    lateinit var weight2: Array<DoubleArray>
    lateinit var weight3: Array<DoubleArray>

    lateinit var bias0: DoubleArray
    lateinit var bias1: DoubleArray
    lateinit var bias2: DoubleArray
    lateinit var bias3: DoubleArray

    init {
        try {
            weight0 = MatrixUtil.loadMatrix("logs/decoder/model.decoder.hidden_layer_1.0.weight.txt", false)
            weight1 = MatrixUtil.loadMatrix("logs/decoder/model.decoder.hidden_layer_2.0.weight.txt", false)
            weight2 = MatrixUtil.loadMatrix("logs/decoder/model.decoder.hidden_layer_3.0.weight.txt", false)
            weight3 = MatrixUtil.loadMatrix("logs/decoder/model.decoder.output_layer.0.weight.txt", false)
            bias0 = MatrixUtil.loadVector("logs/decoder/model.decoder.hidden_layer_1.0.bias.txt")
            bias1 = MatrixUtil.loadVector("logs/decoder/model.decoder.hidden_layer_2.0.bias.txt")
            bias2 = MatrixUtil.loadVector("logs/decoder/model.decoder.hidden_layer_3.0.bias.txt")
            bias3 = MatrixUtil.loadVector("logs/decoder/model.decoder.output_layer.0.bias.txt")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    fun propagate(state: DoubleArray): DoubleArray? {
        var x = MatrixUtil.mmul(state, weight0)
        for (i in x.indices) {
            x[i] = Math.max(x[i] + bias0[i], 0.0)
        }
        x = MatrixUtil.mmul(x, weight1)
        for (i in x.indices) {
            x[i] = Math.max(x[i] + bias1[i], 0.0)
        }
        x = MatrixUtil.mmul(x, weight2)
        for (i in x.indices) {
            x[i] = Math.max(x[i] + bias2[i], 0.0)
        }
        x = MatrixUtil.mmul(x, weight3)
        for (i in x.indices) {
            x[i] += bias3[i]
        }
        return x
    }
}