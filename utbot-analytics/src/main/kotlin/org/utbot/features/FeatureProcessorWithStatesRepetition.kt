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
 * Extract features for state when this state will be marked visited in graph.
 * Add test case when last state of test case will be traversed.
 */
class FeatureProcessorWithStatesRepetition(
    graph: InterProceduralUnitGraph,
    private val saveDir: String = UtSettings.featurePath
) : FeatureProcessor(graph) {
    init {
        File(saveDir).mkdirs()
    }

    companion object {
        private val featureKeys = arrayOf(
            "stack",
            "successor",
            "testCase",
            "coverageByBranch",
            "coverageByPath",
            "depth",
            "cpicnt",
            "icnt",
            "covNew",
            "subpath1",
            "subpath2",
            "subpath4",
            "subpath8"
        )
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
        var currentState: ExecutionState? = executionState

        do {
            val stateHashCode = currentState?.hashCode() ?: 0
            if (currentState?.features?.isEmpty() == true) {
                extractFeatures(currentState)
            }
            currentState?.executingTime?.let { states += (stateHashCode to it) }
            currentState?.features?.let { dumpedStates[stateHashCode] = it }

            currentState?.stmt?.let {
                if (it !in visitedStmts && currentState?.isInNestedMethod() == false) {
                    visitedStmts.add(it)
                    newCoverage++
                }
            }

            currentState = currentState?.parent
        } while (currentState != null)

        generatedTestCases++
        testCases += TestCase(states, newCoverage, generatedTestCases)
    }

    override fun dumpFeatures() {
        val rewards = rewardEstimator.calculateRewards(testCases)

        testCases.forEach { ts ->
            FileOutputStream(
                Paths.get(saveDir, "${UtSettings.testCounter++}.csv").toFile(),
                true
            ).bufferedWriter().use { out ->
                out.appendLine("newCov,reward,${featureKeys.joinToString(separator = ",")}")
                val reversedStates = ts.states.asReversed()

                reversedStates.forEach { (state, _) ->
                    out.appendLine(
                        "${ts.newCoverage != 0},${rewards[state]}," +
                                "${dumpedStates[state]?.joinToString(separator = ",")}"
                    )
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
        val times = mutableMapOf<Int, Long>()

        testCases.forEach { ts ->
            var allTime = 0L
            ts.states.forEach { (state, time) ->
                coverages.compute(state) { _, v ->
                    ts.newCoverage + (v ?: 0)
                }
                val updateTime = !times.contains(state)
                times.compute(state) { _, v ->
                    allTime + (v ?: time)
                }
                if (updateTime) {
                    allTime += time
                }
            }
        }

        coverages.forEach { (state, coverage) ->
            rewards[state] = reward(coverage.toDouble(), times[state]!!.toDouble())
        }

        return rewards
    }

    companion object {
        private const val minTime = 1.0
        private const val rewardDegree = 0.5

        fun reward(coverage: Double, time: Double): Double = (coverage / maxOf(time, minTime)).pow(rewardDegree)
    }
}

data class TestCase(val states: List<Pair<Int, Long>>, val newCoverage: Int, val testIndex: Int)
