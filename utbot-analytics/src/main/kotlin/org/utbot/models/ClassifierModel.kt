package org.utbot.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Properties
import smile.base.mlp.Layer
import smile.base.mlp.OutputFunction
import smile.classification.AdaBoost
import smile.classification.Classifier
import smile.classification.DataFrameClassifier
import smile.classification.DecisionTree
import smile.classification.GradientTreeBoost
import smile.classification.LogisticRegression
import smile.classification.RandomForest
import smile.classification.fisher
import smile.classification.knn
import smile.classification.lda
import smile.classification.mlp
import smile.classification.ovr
import smile.classification.qda
import smile.classification.rda
import smile.classification.svm
import smile.data.CategoricalEncoder
import smile.data.DataFrame
import smile.data.Tuple
import smile.data.formula.Formula
import smile.data.type.StructType
import smile.math.TimeFunction
import smile.math.distance.EuclideanDistance
import smile.math.distance.Metric
import smile.math.kernel.LaplacianKernel
import smile.math.kernel.MercerKernel
import smile.validation.Hyperparameters


@Suppress("unused")
enum class ClassifierModel {
    LOGISTIC {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toIntArray()
            return LogisticRegression.fit(x, y, properties)
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.logistic.lambda", 0.01, 0.1, 0.01)
                .add("smile.logistic.tolerance", doubleArrayOf(0.001, 0.0001, 0.00001))
                .add("smile.logistic.max.iterations", 250, 1000, 250)
    },
    KNN {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val metric: Metric<DoubleArray> = EuclideanDistance()
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toIntArray()
            val k = properties.getProperty("knn.k", "10").toInt()

            return knn(x, y, k, metric)
        }

        // TODO -> SAT-733
        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("knn.k", 5, 40, 1)
    },
    LDA {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toIntArray()
            val tol: Double = properties.getProperty("smile.lda.tolerance", "1E-4").toDouble()

            return lda(x, y, null, tol)
        }

        override fun getHParams(): Hyperparameters {
            val toll = doubleArrayOf(0.001, 0.0001, 0.00001)
            val sampleToll = mutableListOf<Double>()
            for (sample in 1..9) toll.forEach { sampleToll.add(sample * it) }

            return Hyperparameters().add("smile.lda.tolerance", sampleToll.toDoubleArray())
        }
    },
    FISHER {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toIntArray()
            val tol = properties.getProperty("smile.fld.tolerance", "1E-4").toDouble()

            return fisher(x, y, -1, tol)
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.fld.tolerance", doubleArrayOf(0.01, 0.001, 0.0001, 0.00001))
    },
    QDA {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toIntArray()
            val tol = properties.getProperty("smile.qda.tolerance", "1E-4").toDouble()

            return qda(x, y, null, tol)
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.qda.tolerance", doubleArrayOf(0.0001, 0.00001))
    },
    RDA {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toIntArray()
            val alpha = properties.getProperty("smile.rda.alpha", "0.5").toDouble()
            val tol = properties.getProperty("smile.rda.tolerance", "1E-4").toDouble()

            return rda(x, y, alpha, null, tol)
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.rda.alpha", doubleArrayOf(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9))
                .add("smile.rda.tolerance", doubleArrayOf(0.01, 0.001, 0.0001, 0.00001))
    },
    SVM {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toDoubleArray().map { it.toInt() }.toIntArray()
            val tol = properties.getProperty("svm.tol", "1E-3").toDouble()
            val c = properties.getProperty("svm.C", "15.0").toDouble()
            val kernel: MercerKernel<DoubleArray> = LaplacianKernel(0.3)

            return ovr(x, y) { x2, y2 -> svm(x2, y2, kernel, c, tol) }
        }

        // TODO -> SAT-733
        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("svm.C", (1..70).map { it / 2.0 }.toDoubleArray())
                .add("svm.tol", doubleArrayOf(0.01, 0.001, 0.0001, 0.00001))
    },
    CART {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            return ClassifierAdapter(DecisionTree.fit(formula, data, properties), data.schema())
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.cart.split.rule", "GINI")
                .add("smile.cart.max.depth", 5, 30, 5)
                .add("smile.cart.max.nodes", 5, 30, 5)
                .add("smile.cart.node.size", 5, 27, 3)
    },
    RANDOM_FOREST {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            return ClassifierAdapter(RandomForest.fit(formula, data, properties), data.schema())
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.random.forest.trees", 50)
                .add("smile.random.forest.mtry", 0)
                .add("smile.random.forest.split.rule", "GINI")
                .add("smile.random.forest.max.depth", 5, 30, 5)
                .add("smile.random.forest.max.nodes", 5, 30, 5)
                .add("smile.random.forest.node.size", 5, 27, 3)
                .add("smile.random.forest.sample.rate", 0.5, 0.8, 0.1)
    },
    GBM {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            return ClassifierAdapter(GradientTreeBoost.fit(formula, data, properties), data.schema())
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.gbt.trees", 50)
                .add("smile.gbt.max.depth", 5, 30, 5)
                .add("smile.gbt.max.nodes", 5, 30, 5)
                .add("smile.gbt.node.size", 3, 27, 3)
                .add("smile.gbt.shrinkage", 0.01, 0.1, 0.01)
                .add("smile.gbt.sample.rate", 0.4, 0.8, 0.1)
    },
    ADABOOST {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            return ClassifierAdapter(AdaBoost.fit(formula, data, properties), data.schema())
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.adaboost.trees", 30, 50, 10)
                .add("smile.adaboost.max.depth", 5, 30, 5)
                .add("smile.adaboost.max.nodes", 5, 30, 5)
                .add("smile.adaboost.node.size", 3, 27, 3)
    },
    ONE_LAYER_MLP {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toIntArray()

            val neurons = properties.getProperty("smile.mlp.onelayer.neurons", "10").toInt()
            val epochs = properties.getProperty("smile.mlp.onelayer.epochs", "500").toInt()
            val weightDecay = properties.getProperty("smile.mlp.onelayer.weightDecay", "0.005").toDouble()
            val rho = properties.getProperty("smile.mlp.onelayer.rho", "0.005").toDouble()
            val epsilon = properties.getProperty("smile.mlp.onelayer.epsilon", "1E-7").toDouble()
            val momentum = properties.getProperty("smile.mlp.onelayer.momentum", "0.001").toDouble()
            val initialLearningRate = properties.getProperty("smile.mlp.onelayer.initialLearninRate", "0.01").toDouble()
            val learningRateDecay = properties.getProperty("smile.mlp.onelayer.learningRateDecay", "500.0").toDouble()
            val endLearningRate = properties.getProperty("smile.mlp.onelayer.endLearningRate", "0.01").toDouble()

            return mlp(
                    x, y, arrayOf(Layer.sigmoid(neurons), Layer.mle(y.toSet().size, OutputFunction.SIGMOID)), epochs,
                    learningRate = TimeFunction.linear(initialLearningRate, learningRateDecay, endLearningRate),
                    momentum = TimeFunction.constant(momentum),
                    weightDecay = weightDecay, rho = rho, epsilon = epsilon)
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.mlp.onelayer.neurons", 4, 16, 2)
                .add("smile.mlp.onelayer.epochs", 100)
                .add("smile.mlp.onelayer.weightDecay", doubleArrayOf(0.005, 0.0001, 0.00005))
                .add("smile.mlp.onelayer.rho", doubleArrayOf(0.005, 0.0001, 0.00005))
                .add("smile.mlp.onelayer.epsilon", doubleArrayOf(0.005, 0.0001, 0.00005))
                .add("smile.mlp.onelayer.momentum", doubleArrayOf(0.005, 0.0001, 0.00005))
                .add("smile.mlp.onelayer.initialLearninRate", doubleArrayOf(0.01, 0.001, 0.0001, 0.00001))
                .add("smile.mlp.onelayer.learningRateDecay", 100.0)
                .add("smile.mlp.onelayer.endLearningRate", doubleArrayOf(0.1, 0.01))
    },
    TWO_LAYER_MLP {
        override fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray> {
            smile.math.MathEx.setSeed(CLASSIFIER_MODEL_SEED)
            val xFrame = formula.x(data)
            val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
            val y = formula.y(data).toIntArray()

            val firstLayerNeurons = properties.getProperty("smile.mlp.twolayer.first_layer", "12").toInt()
            val secondLayerNeurons = properties.getProperty("smile.mlp.twolayer.second_layer", "8").toInt()

            val epochs = properties.getProperty("smile.mlp.twolayer.epochs", "500").toInt()
            val weightDecay = properties.getProperty("smile.mlp.twolayer.weightDecay", "0.005").toDouble()
            val rho = properties.getProperty("smile.mlp.twolayer.rho", "0.005").toDouble()
            val epsilon = properties.getProperty("smile.mlp.twolayer.epsilon", "1E-7").toDouble()
            val momentum = properties.getProperty("smile.mlp.twolayer.momentum", "0.001").toDouble()
            val initialLearningRate = properties.getProperty("smile.mlp.twolayer.initialLearninRate", "0.01").toDouble()
            val learningRateDecay = properties.getProperty("smile.mlp.twolayer.learningRateDecay", "500.0").toDouble()
            val endLearningRate = properties.getProperty("smile.mlp.twolayer.endLearningRate", "0.01").toDouble()

            return mlp(
                    x,
                    y,
                    arrayOf(
                            Layer.sigmoid(firstLayerNeurons),
                            Layer.sigmoid(secondLayerNeurons),
                            Layer.mle(y.toSet().size, OutputFunction.SIGMOID)
                    ),
                    epochs = epochs,
                    learningRate = TimeFunction.linear(initialLearningRate, learningRateDecay, endLearningRate),
                    momentum = TimeFunction.constant(momentum),
                    weightDecay = weightDecay,
                    rho = rho,
                    epsilon = epsilon
            )
        }

        override fun getHParams(): Hyperparameters = Hyperparameters()
                .add("smile.mlp.twolayer.first_layer", 4, 16, 2)
                .add("smile.mlp.twolayer.second_layer", 4, 16, 2)
                .add("smile.mlp.twolayer.epochs", 100)
                .add("smile.mlp.twolayer.weightDecay", doubleArrayOf(0.005, 0.0001, 0.00005))
                .add("smile.mlp.twolayer.rho", doubleArrayOf(0.005, 0.0001, 0.00005))
                .add("smile.mlp.twolayer.epsilon", doubleArrayOf(0.005, 0.0001, 0.00005))
                .add("smile.mlp.twolayer.momentum", doubleArrayOf(0.005, 0.0001, 0.00005))
                .add("smile.mlp.twolayer.initialLearninRate", doubleArrayOf(0.01, 0.001, 0.0001, 0.00001))
                .add("smile.mlp.twolayer.learningRateDecay", 100.0)
                .add("smile.mlp.twolayer.endLearningRate", doubleArrayOf(0.1, 0.01))
    };

    abstract fun train(formula: Formula, data: DataFrame, properties: Properties): Classifier<DoubleArray>
    abstract fun getHParams(): Hyperparameters
}

const val CLASSIFIER_MODEL_SEED = 19650218L

class ClassifierAdapter(private val model: DataFrameClassifier,
                        private val schema: StructType) : Classifier<DoubleArray> {
    companion object {
        @JvmStatic
        private val serialVersionUID = 1L
    }

    override fun predict(x: DoubleArray?): Int = this.model.predict(Tuple.of(x, this.schema))
}

class DataFrameClassifierAdapter(private val model: Classifier<DoubleArray>,
                                 private val formula: Formula,
                                 private val schema: StructType) : DataFrameClassifier {

    override fun predict(x: Tuple?): Int {
        return model.predict(x?.toArray())
    }

    override fun schema(): StructType {
        return this.schema
    }

    override fun formula(): Formula {
        return this.formula
    }
}


fun loadModelFromJson(name: String, modelName: String): Map<String, String> {
    InputStreamReader(Thread.currentThread().contextClassLoader.getResourceAsStream(name)!!).use { reader ->
        val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
        val models: Map<String, Map<String, String>> = Gson().fromJson(reader, type)

        return models[modelName] ?: emptyMap()
    }
}

fun Properties.saveToJson(path: String, modelName: String) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    var models: MutableMap<String, Map<String, String>>? = null

    FileReader(path).use { reader ->
        val type = object : TypeToken<MutableMap<String, Map<String, String>>>() {}.type
        val newData: MutableMap<String, String> = mutableMapOf()
        this.stringPropertyNames().forEach { newData[it] = this.getProperty(it) }

        models = gson.fromJson(reader, type)
        models!!.put(modelName, newData)
    }

    FileWriter(path).use { writer ->
        gson.toJson(models, writer)
    }
}

fun <T> save(model: T, path: String) {
    ObjectOutputStream(FileOutputStream(path)).use {
        it.writeObject(model)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> loadModel(name: String): T =
// Uncomment for local debug (after model training)
//        ObjectInputStream(FileInputStream("logs/$name")).use {
        ObjectInputStream(Thread.currentThread().contextClassLoader.getResourceAsStream("models/$name")).use {
            it.readObject() as T
        }
