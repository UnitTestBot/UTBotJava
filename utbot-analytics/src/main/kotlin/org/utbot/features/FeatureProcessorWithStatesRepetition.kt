package org.utbot.features

import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.analytics.FeatureProcessor
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.framework.UtSettings
import soot.jimple.Stmt
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import kotlin.math.pow

/**
 * Implementation of feature processor, in which we dump each test, so there will be several copies of each state.
 * Goal is make weighted dataset, where more value for states, which generated more tests.
 * Extract features for state when this state will be marked visited in graph.
 * Add test case, when last state of it will be traversed.
 *
 * @param graph execution graph of current symbolic traverse
 * @param saveDir directory in which we  will store features and rewards of [ExecutionState]
 */
class FeatureProcessorWithStatesRepetition(
    graph: InterProceduralUnitGraph,
    private val saveDir: String = UtSettings.featurePath
) : FeatureProcessor(graph) {
    init {
        File(saveDir).mkdirs()
    }

    companion object {
        private const val featureFile = "jlearch_features"
        private val featureKeys = Companion::class.java.classLoader.getResourceAsStream(featureFile)
            ?.bufferedReader().use {
                it?.readText()?.split(System.lineSeparator()) ?: emptyList()
            }
    }

    private var generatedTestCases = 0
    private val featureExtractor = EngineAnalyticsContext.featureExtractorFactory(graph)
    private val rewardEstimator = RewardEstimator()

    private val dumpedStates = mutableMapOf<Int, List<Double>>()
    private val visitedStmts = mutableSetOf<Stmt>()
    private val testCases = mutableListOf<TestCase>()

    private fun extractFeatures(executionState: ExecutionState) {
        featureExtractor.extractFeatures(executionState, generatedTestCases)
    }

    private fun addTestCase(executionState: ExecutionState) {
        val states = mutableListOf<Pair<Int, Long>>()
        var newCoverage = 0

        generateSequence(executionState) { currentState ->
            val stateHashCode = currentState.hashCode()

            if (currentState.features.isEmpty()) {
                extractFeatures(currentState)
            }

            states += stateHashCode to currentState.executingTime
            dumpedStates[stateHashCode] = currentState.features

            currentState.stmt.let {
                if (it !in visitedStmts && !currentState.isInNestedMethod()) {
                    visitedStmts += it
                    newCoverage++
                }
            }

            currentState.parent
        }

        generatedTestCases++
        testCases += TestCase(states, newCoverage, generatedTestCases)
    }

    override fun dumpFeatures() {
        val rewards = rewardEstimator.calculateRewards(testCases)

        testCases.forEach { ts ->
            val outputFile = Paths.get(saveDir, "${UtSettings.testCounter++}.csv").toFile()
            FileOutputStream(outputFile, true)
                .bufferedWriter()
                .use { out ->
                    out.appendLine("newCov,reward,${featureKeys.joinToString(separator = ",")}")
                    val reversedStates = ts.states.asReversed()

                    reversedStates.forEach { (state, _) ->
                        val isCoveredNew = ts.newCoverage != 0
                        val reward = rewards[state]
                        val features = dumpedStates[state]?.joinToString(separator = ",")

                        out.appendLine("$isCoveredNew,$reward,$features")
                    }

                    out.flush()
                }
        }
    }

    override fun onTraversed(executionState: ExecutionState) {
        addTestCase(executionState)
    }

    override fun onVisit(executionState: ExecutionState) {
        extractFeatures(executionState)
    }
}

internal class RewardEstimator {

    fun calculateRewards(testCases: List<TestCase>): Map<Int, Double> {
        val rewards = mutableMapOf<Int, Double>()
        val coverages = mutableMapOf<Int, Int>()
        val stateToExecutingTime = mutableMapOf<Int, Long>()

        testCases.forEach { ts ->
            var allTime = 0L
            ts.states.forEach { (stateHash, time) ->
                coverages.compute(stateHash) { _, v ->
                    ts.newCoverage + (v ?: 0)
                }
                val isNewState = stateHash !in stateToExecutingTime
                stateToExecutingTime.compute(stateHash) { _, v ->
                    allTime + (v ?: time)
                }
                if (isNewState) {
                    allTime += time
                }
            }
        }

        coverages.forEach { (state, coverage) ->
            rewards[state] = reward(coverage.toDouble(), stateToExecutingTime.getValue(state).toDouble())
        }

        return rewards
    }

    companion object {
        /**
         * Threshold for time: executingTime less than that we don't distinct. We are not expiremented with changing it yet,
         * now it is just minimal positive value distinct from 0.
         */
        private const val minTime = 1.0

        /**
         * Just degree of reward to make it smaller if it more than 1 and bigger if it less than 1.
         */
        private const val rewardDegree = 0.5

        fun reward(coverage: Double, time: Double): Double = (coverage / maxOf(time, minTime)).pow(rewardDegree)
    }
}

/**
 * Class that represents test case.
 * @param states pairs from stateHash and executingTime, created from each state of this test case
 * @param newCoverage number of instructions, that was visited in first time by [states]
 * @param testIndex number of test case, that was created before
 */
data class TestCase(val states: List<Pair<Int, Long>>, val newCoverage: Int, val testIndex: Int)
