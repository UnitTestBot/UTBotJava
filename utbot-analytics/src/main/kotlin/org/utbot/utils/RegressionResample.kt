package org.utbot.utils

import org.utbot.utils.ResampleGenerationStrategy.CUT
import org.utbot.utils.ResampleGenerationStrategy.NO_GENERATION
import org.utbot.utils.ResampleSplitStrategy.*
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.math.MathEx


/**
 * Strategies for split continues values into groups
 */
enum class ResampleSplitStrategy {
    NO_SPLIT,
    CUSTOM,
    REL_FUN
}

/**
 * Group balancing strategies.
 */
enum class ResampleGenerationStrategy {
    NO_GENERATION,
    CUT
}


class RegressionResample(private val formula: Formula) {

    private var valueIntervals: MutableList<Double> = mutableListOf()
    private var generationStrategy: ResampleGenerationStrategy = NO_GENERATION

    fun fit(
            dataframe: DataFrame,
            splitStrategy: ResampleSplitStrategy,
            generationStrategy: ResampleGenerationStrategy
    ) {
        this.generationStrategy = generationStrategy
        this.valueIntervals = when (splitStrategy) {
            NO_SPLIT -> singleInterval(dataframe, formula)
            CUSTOM -> customIntervals()
            REL_FUN -> mutableListOf() // TODO
        }
    }

    private fun customIntervals(): MutableList<Double> =
            mutableListOf(0.0, 6.0, 10.0, 15.0, 20.0, 60.0, 100.0, 150.0, 800.0)

    private fun singleInterval(dataframe: DataFrame, formula: Formula): MutableList<Double> {
        val values = formula.y(dataframe).toDoubleArray()
        values.minOrNull().let {
            return mutableListOf(it!! - 0.01)
        }
    }

    fun groupIndicesByLabel(dataframe: DataFrame): Array<MutableList<Int>> {
        val result = Array<MutableList<Int>>(valueIntervals.size) { mutableListOf() }
        val sortedIndexedValue = formula.y(dataframe).toDoubleArray().withIndex().sortedBy { it.value }
        var intervalIndex = 0

        sortedIndexedValue.forEach {
            while (intervalIndex < valueIntervals.size && it.value > valueIntervals[intervalIndex]) {
                intervalIndex++
            }
            result[intervalIndex - 1].add(it.index)
        }

        return result
    }

    fun transform(dataframe: DataFrame): DataFrame {
        if (this.generationStrategy == NO_GENERATION) return dataframe
        val intervalsIndexes = groupIndicesByLabel(dataframe)
        val result = mutableListOf<DoubleArray>()

        if (generationStrategy == CUT) {
            intervalsIndexes.forEach {
                if (it.size > 500) {
                    val indexes = it.toIntArray()
                    MathEx.permutate(indexes)
                    result.addAll(MathEx.slice(dataframe.toArray(), indexes.slice(0..500).toIntArray()))
                } else {
                    val indexes = it.toIntArray()
                    MathEx.permutate(indexes)
                    result.addAll(MathEx.slice(dataframe.toArray(), indexes))
                }
            }
        }
        result.shuffle()
        return DataFrame.of(result.toTypedArray(), *dataframe.names())
    }
}