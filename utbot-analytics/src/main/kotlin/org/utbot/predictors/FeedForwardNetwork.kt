package org.utbot.predictors

import smile.math.matrix.Matrix
import kotlin.math.max

private object ActivationFunctions {
    const val ReLU = "reLU"
}

data class FeedForwardNetwork(val operations: List<(DoubleArray) -> DoubleArray>)

private fun reLU(input: DoubleArray): DoubleArray {
    return input.map { max(0.0, it) }.toDoubleArray()
}

internal fun buildModel(nnJson: NNJson): FeedForwardNetwork {
    val weights = nnJson.linearLayers.map { Matrix(it) }
    val biases = nnJson.biases.map { Matrix(it) }
    val operations = mutableListOf<(DoubleArray) -> DoubleArray>()

    nnJson.linearLayers.indices.forEach { i ->
        operations.add {
            weights[i].mm(Matrix(it)).add(biases[i]).col(0)
        }
        if (i != nnJson.linearLayers.lastIndex) {
            operations.add {
                when (nnJson.activationLayers[i]) {
                    ActivationFunctions.ReLU -> reLU(it)
                    else -> error("Unsupported activation")
                }
            }
        }
    }

    return FeedForwardNetwork(operations)
}