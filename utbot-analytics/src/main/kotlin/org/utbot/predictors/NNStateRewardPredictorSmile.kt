package org.utbot.predictors

import com.google.gson.Gson
import org.utbot.framework.UtSettings
import java.io.FileReader
import java.nio.file.Paths
import kotlin.math.max
import smile.math.matrix.Matrix

data class NNJson(
    val linearLayers: Array<Array<DoubleArray>> = arrayOf(),
    val activationLayers: Array<String> = arrayOf(),
    val biases: Array<DoubleArray> = arrayOf()
)

data class NN(val operations: List<(DoubleArray) -> DoubleArray>)

private fun reLU(input: DoubleArray): DoubleArray {
    return input.map { max(0.0, it) }.toDoubleArray()
}

private fun loadNN(path: String): NN {
    val nnJson: NNJson = Gson().fromJson(FileReader(Paths.get(UtSettings.rewardModelPath, path).toFile()), NNJson::class.java) ?: run {
        System.err.println("Something went wrong while parsing NN model")
        NNJson()
    }

    val weights = nnJson.linearLayers.map { Matrix(it) }
    val biases = nnJson.biases.map { Matrix(it) }
    val operations = mutableListOf<(DoubleArray) -> DoubleArray>()

    (0 until nnJson.linearLayers.size).forEach { i ->
        operations.add {
            weights[i].mm(Matrix(it)).add(biases[i]).col(0)
        }
        if (i != nnJson.linearLayers.size - 1) {
            operations.add {
                when (nnJson.activationLayers[i]) {
                    "reLU" -> reLU(it)
                    else -> error("Unsupported activation")
                }
            }
        }
    }

    return NN(operations)
}

data class StandardScaler(val mean: Matrix?, val variance: Matrix?)

internal fun loadScaler(path: String): StandardScaler =
    Paths.get(UtSettings.rewardModelPath, path).toFile().bufferedReader().use {
        val mean = it.readLine()?.split(',')?.map(String::toDouble)?.toDoubleArray()
        val variance = it.readLine()?.split(',')?.map(String::toDouble)?.toDoubleArray()
        StandardScaler(Matrix(mean), Matrix(variance))
    }

class NNStateRewardPredictorSmile(modelPath: String = "nn.json", scalerPath: String = "scaler.txt") : NNStateRewardPredictor {
    val nn = loadNN(modelPath)
    val scaler = loadScaler(scalerPath)

    override fun predict(input: List<Double>): Double {
        var inputArray = input.toDoubleArray()
        inputArray = Matrix(inputArray).sub(scaler.mean).div(scaler.variance).col(0)

        nn.operations.forEach {
            inputArray = it(inputArray)
        }

        check(inputArray.size == 1)
        return inputArray[0]
    }
}
