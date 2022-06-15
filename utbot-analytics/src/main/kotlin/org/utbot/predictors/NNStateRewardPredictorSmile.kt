package org.utbot.predictors

import com.google.gson.Gson
import org.utbot.framework.UtSettings
import smile.math.matrix.Matrix
import java.io.FileReader
import java.nio.file.Paths
import kotlin.math.max

private const val DEFAULT_MODEL_PATH = "nn.json"
private const val DEFAULT_SCALER_PATH = "scaler.txt"

private object ActivationFunctions {
    const val ReLU = "reLU"
}

data class NNJson(
    val linearLayers: Array<Array<DoubleArray>> = emptyArray(),
    val activationLayers: Array<String> = emptyArray(),
    val biases: Array<DoubleArray> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NNJson

        if (!linearLayers.contentDeepEquals(other.linearLayers)) return false
        if (!activationLayers.contentEquals(other.activationLayers)) return false
        if (!biases.contentDeepEquals(other.biases)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = linearLayers.contentDeepHashCode()
        result = 31 * result + activationLayers.contentHashCode()
        result = 31 * result + biases.contentDeepHashCode()
        return result
    }
}

data class NeuralNetwork(val operations: List<(DoubleArray) -> DoubleArray>)

private fun reLU(input: DoubleArray): DoubleArray {
    return input.map { max(0.0, it) }.toDoubleArray()
}

private fun loadModel(path: String): NNJson {
    val modelFile = Paths.get(UtSettings.rewardModelPath, path).toFile()
    val nnJson: NNJson =
        Gson().fromJson(FileReader(modelFile), NNJson::class.java) ?: run {
            error("Empty model")
        }

    return nnJson
}

private fun buildModel(nnJson: NNJson): NeuralNetwork {
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

    return NeuralNetwork(operations)
}

private fun getModel(path: String) = buildModel(loadModel(path))

data class StandardScaler(val mean: Matrix?, val variance: Matrix?)

internal fun loadScaler(path: String): StandardScaler =
    Paths.get(UtSettings.rewardModelPath, path).toFile().bufferedReader().use {
        val mean = it.readLine()?.splitByCommaIntoDoubleArray() ?: error("There is not mean in $path")
        val variance = it.readLine()?.splitByCommaIntoDoubleArray() ?: error("There is not variance in $path")
        StandardScaler(Matrix(mean), Matrix(variance))
    }

class NNStateRewardPredictorSmile(modelPath: String = DEFAULT_MODEL_PATH, scalerPath: String = DEFAULT_SCALER_PATH) :
    NNStateRewardPredictor {
    private val nn = getModel(modelPath)
    private val scaler = loadScaler(scalerPath)

    override fun predict(input: List<Double>): Double {
        var inputArray = input.toDoubleArray()
        inputArray = Matrix(inputArray).sub(scaler.mean).div(scaler.variance).col(0)

        nn.operations.forEach {
            inputArray = it(inputArray)
        }

        check(inputArray.size == 1) { "Neural network have several outputs" }
        return inputArray[0]
    }
}
