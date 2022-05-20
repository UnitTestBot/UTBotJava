package org.utbot.features

import org.utbot.models.ClassifierModel
import org.utbot.models.DataFrameClassifierAdapter
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.feature.*
import smile.math.MathEx
import smile.math.matrix.Matrix
import smile.projection.GHA
import smile.projection.LinearProjection
import smile.projection.PCA
import smile.projection.ProbabilisticPCA
import java.io.Serializable
import java.util.*

interface Transform : Serializable {
    fun apply(x: DoubleArray): DoubleArray
    fun apply(data: DataFrame): DataFrame
}

enum class ProjectionType : Serializable {
    PCA, PPCA, GHA
}

enum class ScalingType : Serializable {
    MAXMIN, WINSOR, STANDARD
}

class Projection(private val type: ProjectionType,
                 private val formula: Formula,
                 private val matrix: Matrix) : Transform, LinearProjection {

    companion object {
        fun initPCA(formula: Formula, data: DataFrame): Projection {
            val matrix = PCA.fit(formula.x(data).toArray()).projection
            return Projection(ProjectionType.PCA, formula, matrix)
        }

        fun initPPCA(formula: Formula, data: DataFrame, k: Int = 30): Projection {
            val matrix = ProbabilisticPCA.fit(formula.x(data).toArray(), k).projection
            return Projection(ProjectionType.PPCA, formula, matrix)
        }

        fun initGHA(formula: Formula, data: DataFrame, p: Int = 20, lr: Double = 0.00001): Projection {
            val numInput = data.schema().fields().size - 1
            val features = formula.x(data).toArray()
            val gha = GHA(numInput, p, lr)
            for (feature in features)
                gha.update(feature)
            return Projection(ProjectionType.GHA, formula, gha.projection)
        }
    }

    override fun apply(x: DoubleArray): DoubleArray {
        return this.project(x)
    }

    override fun apply(data: DataFrame): DataFrame {
        val features = formula.x(data)
        val labels = formula.y(data)
        val projectedFeatures = this.project(features.toArray())
        val projectedFeaturesFrame = DataFrame.of(projectedFeatures, *features.names())

        return projectedFeaturesFrame.merge(labels)
    }

    override fun getProjection(): Matrix {
        return matrix
    }

    override fun toString(): String {
        return "Projection[${this.type.name}]"
    }
}

class Scaling(private val type: ScalingType,
              private val formula: Formula,
              private val featureTransform: FeatureTransform) : Transform {

    companion object {
        fun initMaxMin(formula: Formula, data: DataFrame): Scaling {
            val featureTransform = MaxAbsScaler.fit(formula.x(data).toArray())
            return Scaling(ScalingType.MAXMIN, formula, featureTransform)
        }

        fun initWinsor(formula: Formula, data: DataFrame,
                       lowerLimit: Double = 0.05, upperLimit: Double = 0.95): Scaling {
            val featureTransform = WinsorScaler.fit(formula.x(data).toArray(), lowerLimit, upperLimit)
            return Scaling(ScalingType.WINSOR, formula, featureTransform)
        }

        fun initStandard(formula: Formula, data: DataFrame): Scaling {
            val featureTransform = Standardizer.fit(formula.x(data).toArray())
            return Scaling(ScalingType.STANDARD, formula, featureTransform)
        }
    }

    override fun apply(x: DoubleArray): DoubleArray {
        return featureTransform.transform(x)
    }

    override fun apply(data: DataFrame): DataFrame {
        val features: DataFrame = formula.x(data)
        val labels = formula.y(data)
        val scaledFeatures = featureTransform.transform(features.toArray())
        val scaledFeaturesFrame = DataFrame.of(scaledFeatures, *features.names())
        return scaledFeaturesFrame.merge(labels)
    }

    override fun toString(): String {
        return "Scaling[${type.name}]"
    }
}

class Compose(private val transforms: Iterable<Transform>) : Transform {
    override fun apply(x: DoubleArray): DoubleArray {
        return transforms.fold(x) { acc, transform -> transform.apply(acc) }
    }

    override fun apply(data: DataFrame): DataFrame {
        return transforms.fold(data) { acc, transform -> transform.apply(acc) }
    }

    override fun toString(): String {
        return "Compose[\n\t" + transforms.joinToString(separator = "\n\t") { it.toString() } + "\n]"
    }
}

class FeatureSelection : Transform {

    private val saveIndexes: List<Int>
    private val formula: Formula
    private val name: String

    constructor(dataTrain: DataFrame, dataVal: DataFrame, formula: Formula,
                classifierModel: ClassifierModel, properties: Properties = Properties()) {
        val result = GAFE().apply(
                100, 1, formula.x(dataTrain).ncols(),
                GAFE.fitness("EXEC_TIME", dataTrain, dataVal, smile.validation.metric.Accuracy(),
                        { x, y -> DataFrameClassifierAdapter(classifierModel.train(x, y, properties), x, y.schema()) })
        )
        saveIndexes = result.maxByOrNull { it.fitness() }?.bits()?.withIndex()?.filter { it.value.toInt() == 1 }?.map { it.index }!!
        this.formula = formula
        this.name = "GAFE"
    }

    constructor(formula: Formula, data: DataFrame) {
        val features: Matrix = formula.matrix(data, false)
        val scale = features.colSds()
        this.saveIndexes = scale.indices.filter { !MathEx.isZero(scale[it]) }
        this.formula = formula
        this.name = "RemoveConst"
    }

    override fun apply(x: DoubleArray): DoubleArray {
        return saveIndexes.map { x[it] }.toDoubleArray()
    }

    override fun apply(data: DataFrame): DataFrame {
        val colNames = formula.x(data).names()
        return data.drop(*colNames.filterIndexed { index, _ -> index !in saveIndexes }.toTypedArray())
    }

    override fun toString(): String {
        return "FeatureSelection[${name}]"
    }
}
