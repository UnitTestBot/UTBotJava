package org.utbot.predictors

import org.utbot.framework.UtSettings
import smile.math.matrix.Matrix
import java.nio.file.Paths

data class StandardScaler(val mean: Matrix?, val variance: Matrix?)

internal fun loadScaler(path: String): StandardScaler =
    Paths.get(UtSettings.rewardModelPath, path).toFile().bufferedReader().use {
        val mean = it.readLine()?.splitByCommaIntoDoubleArray() ?: error("There is not mean in $path")
        val variance = it.readLine()?.splitByCommaIntoDoubleArray() ?: error("There is not variance in $path")
        StandardScaler(Matrix(mean), Matrix(variance))
    }
