package org.utbot.utils.layers

import org.utbot.utils.MatrixUtil
import java.io.FileNotFoundException

class MessageAggregation {

    lateinit var weight0: Array<DoubleArray>
    lateinit var weight1: Array<DoubleArray>

    init {
        try {
            weight0 = MatrixUtil.loadMatrix("logs/model.encoder.x_update.0.weight", false)
            weight1 = MatrixUtil.loadMatrix("logs/model.encoder.x_update.2.weight", false)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    fun propagate(state: DoubleArray, message: DoubleArray): DoubleArray {
        val x = MatrixUtil.mmul(message, state, weight0)
        for (i in x.indices) {
            x[i] = Math.max(x.get(i), 0.0)
        }
        return MatrixUtil.mmul(x, weight1)
    }
}