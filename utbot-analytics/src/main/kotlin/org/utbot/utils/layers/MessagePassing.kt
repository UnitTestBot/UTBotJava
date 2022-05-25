package org.utbot.utils.layers

import org.utbot.utils.MatrixUtil
import java.io.FileNotFoundException
import java.util.ArrayList


class MessagePassing {

    lateinit var weight: Array<DoubleArray>

    init {
        try {
            weight = MatrixUtil.loadMatrix("logs/model.encoder.weight.txt", true)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    fun propagate(message: DoubleArray): DoubleArray {
        return MatrixUtil.mmul(message, weight)
    }

    fun propagate(messages: ArrayList<DoubleArray>): DoubleArray {
        return MatrixUtil.mmulSum(messages, weight)
    }
}