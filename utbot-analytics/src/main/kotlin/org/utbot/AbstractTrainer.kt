package org.utbot

import org.utbot.features.*
import org.utbot.utils.RegressionResample
import org.utbot.utils.ResampleGenerationStrategy
import org.utbot.utils.ResampleSplitStrategy
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.math.MathEx
import smile.projection.PCA

private const val TARGET_NAME = "EXEC_TIME"

abstract class AbstractTrainer(
        var data: DataFrame,
        private val featureScaling: ScalingType? = ScalingType.WINSOR,
        private val featureProjection: ProjectionType? = null,
        val targetColumn: String = TARGET_NAME,
        resampleSplitStrategy: ResampleSplitStrategy = ResampleSplitStrategy.CUSTOM,
        resampleGenerationStrategy: ResampleGenerationStrategy = ResampleGenerationStrategy.CUT,
        private val removeConstColumns: Boolean = false,
        private val savePcaVariance: Boolean = false,
        trainProportion: Double = 0.8
) {
    var trainData: DataFrame
    val formula: Formula = Formula.lhs(targetColumn)
    var validationData: DataFrame
    val resample: RegressionResample = RegressionResample(formula)

    var pcaCumulativeVarianceProportion = doubleArrayOf()
    var pcaVarianceProportion = doubleArrayOf()
    var transforms = mutableListOf<Transform>()

    init {
        val threshold = (data.size() * trainProportion).toInt()
        val (trainIndexes, valIndexes) = MathEx.permutate(data.size()).toList().chunked(threshold)

        trainData = DataFrame.of(MathEx.slice(data.toArray(), trainIndexes.toIntArray()), *data.names())
        validationData = DataFrame.of(MathEx.slice(data.toArray(), valIndexes.toIntArray()), *data.names())

        resample.fit(trainData, resampleSplitStrategy, resampleGenerationStrategy)
        trainData = resample.transform(trainData)
        validationData = resample.transform(validationData)
    }

    private fun scaleData() {
        if (featureScaling == null) {
            return
        }

        transforms.add(when (featureScaling) {
            ScalingType.MAXMIN -> Scaling.initMaxMin(formula, trainData)
            ScalingType.WINSOR -> Scaling.initWinsor(formula, trainData)
            ScalingType.STANDARD -> Scaling.initStandard(formula, trainData)
        })
        trainData = transforms.last().apply(trainData)
    }

    private fun projectData() {
        if (featureProjection == null) {
            return
        }
        transforms.add(when (featureProjection) {
            ProjectionType.PCA -> Projection.initPCA(formula, trainData)
            ProjectionType.PPCA -> Projection.initPPCA(formula, trainData)
            ProjectionType.GHA -> Projection.initGHA(formula, trainData)
        })
        trainData = transforms.last().apply(trainData)

        if (savePcaVariance) {
            val pca = PCA.fit(formula.x(trainData).toArray())
            pcaCumulativeVarianceProportion = pca.cumulativeVarianceProportion
            pcaVarianceProportion = pca.varianceProportion
        }
    }

    fun preprocess() {
        if (removeConstColumns) {
            transforms.add(FeatureSelection(formula, trainData))
            trainData = transforms.last().apply(trainData)
        }
        scaleData()
        featureSelection()
        projectData()
    }

    fun fit() {
        preprocess()
        train()
        validate()
        visualize()
        save()
    }

    abstract fun train()
    abstract fun validate()
    abstract fun visualize()
    abstract fun featureSelection()
    abstract fun save()
}
