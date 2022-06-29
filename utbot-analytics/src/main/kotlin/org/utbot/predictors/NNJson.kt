package org.utbot.predictors

import com.google.gson.Gson
import org.utbot.framework.UtSettings
import org.utbot.predictors.util.ModelLoadingException
import java.io.FileReader
import java.nio.file.Paths

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

internal fun loadModel(path: String): NNJson {
    val modelFile = Paths.get(UtSettings.rewardModelPath, path).toFile()
    lateinit var nnJson: NNJson

    try {
        nnJson =
            Gson().fromJson(FileReader(modelFile), NNJson::class.java) ?: run {
                error("Empty model")
            }
    } catch (e: Exception) {
        throw ModelLoadingException(e)
    }

    return nnJson
}
