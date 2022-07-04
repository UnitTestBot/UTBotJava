package org.utbot.predictors

import org.utbot.framework.UtSettings
import org.utbot.predictors.util.ScalerLoadingException
import org.utbot.predictors.util.splitByCommaIntoDoubleArray
import smile.math.matrix.Matrix
import java.nio.file.Paths


internal const val DEFAULT_SCALER_PATH = "scaler.txt"

data class StandardScaler(val mean: Matrix?, val variance: Matrix?)

internal fun loadScaler(path: String): StandardScaler =
    try {
        Paths.get(UtSettings.rewardModelPath, path).toFile().bufferedReader().use {
            val mean = it.readLine()?.splitByCommaIntoDoubleArray() ?: error("There is not mean in $path")
            val variance = it.readLine()?.splitByCommaIntoDoubleArray() ?: error("There is not variance in $path")
            StandardScaler(Matrix(mean), Matrix(variance))
        }
    } catch (e: Exception) {
        throw ScalerLoadingException(e)
    }
