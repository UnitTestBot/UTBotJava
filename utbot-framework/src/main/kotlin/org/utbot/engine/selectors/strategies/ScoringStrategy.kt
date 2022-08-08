package org.utbot.engine.selectors.strategies

import kotlinx.collections.immutable.PersistentList
import mu.KotlinLogging
import org.utbot.engine.*
import org.utbot.engine.pc.UtSolverStatus
import org.utbot.engine.pc.UtSolverStatusSAT
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import soot.jimple.Stmt
import soot.toolkits.graph.ExceptionalUnitGraph
import kotlin.math.abs

abstract class ScoringStrategy(graph: InterProceduralUnitGraph) : TraverseGraphStatistics(graph), ChoosingStrategy {
    abstract fun score(executionState: ExecutionState): Double

    operator fun get(state: ExecutionState): Double = score(state)
}

class ScoringStrategyBuilder(
    private val targets: Map<LocalVariable, UtModel>
) {

    constructor() : this(emptyMap())

    fun build(graph: InterProceduralUnitGraph, typeRegistry: TypeRegistry): ScoringStrategy =
        ModelSynthesisScoringStrategy(graph, targets, typeRegistry)
}

val defaultScoringStrategy get() = ScoringStrategyBuilder(emptyMap())


private typealias Path = PersistentList<Stmt>

class ModelSynthesisScoringStrategy(
    graph: InterProceduralUnitGraph,
    private val targets: Map<LocalVariable, UtModel>,
    private val typeRegistry: TypeRegistry
) : ScoringStrategy(graph) {
    private val logger = KotlinLogging.logger("ModelSynthesisScoringStrategy")
    private val distanceStatistics = DistanceStatistics(graph)

    companion object {
        private const val SOFT_MAX_ARRAY_SIZE = 40
        private const val DEPTH_CHECK = 10

        private const val PATH_SCORE_COEFFICIENT = 1.0
        private const val MODEL_SCORE_COEFFICIENT = 100.0

        private const val INF_SCORE = Double.MAX_VALUE
        private const val MAX_SCORE = 1.0
        private const val EPS = 0.01
    }

    // needed for resolver
    private val hierarchy = Hierarchy(typeRegistry)
    private val typeResolver: TypeResolver = TypeResolver(typeRegistry, hierarchy)

    private val stateModels = hashMapOf<ExecutionState, UtSolverStatus>()
    private val pathScores = hashMapOf<Path, Double>()

    private fun buildResolver(memory: Memory, holder: UtSolverStatusSAT) =
        Resolver(hierarchy, memory, typeRegistry, typeResolver, holder, "", SOFT_MAX_ARRAY_SIZE)

    override fun onTraversed(executionState: ExecutionState) {
        distanceStatistics.onTraversed(executionState)
    }

    override fun onVisit(edge: Edge) {
        distanceStatistics.onVisit(edge)
    }

    override fun onVisit(executionState: ExecutionState) {
        distanceStatistics.onVisit(executionState)
    }

    override fun onJoin(stmt: Stmt, graph: ExceptionalUnitGraph, shouldRegister: Boolean) {
        distanceStatistics.onJoin(stmt, graph, shouldRegister)
    }

    private fun shouldDropBasedOnScores(state: ExecutionState): Boolean {
        val previous = run {
            var current = state.path
            val res = mutableListOf<Path>()
            repeat(DEPTH_CHECK) {
                if (current.isEmpty()) return@repeat
                res += current
                current = current.removeAt(current.lastIndex)
            }
            res.reversed()
        }
        val scores = previous.map { pathScores.getOrDefault(it, INF_SCORE) }
        return scores.size >= DEPTH_CHECK && (0 until scores.lastIndex).all { scores[it] <= scores[it + 1] }
    }

    override fun shouldDrop(state: ExecutionState): Boolean {
        return shouldDropBasedOnScores(state) || distanceStatistics.shouldDrop(state)
    }

    override fun score(executionState: ExecutionState): Double = pathScores.getOrPut(executionState.path) {
        computePathScore(executionState) * PATH_SCORE_COEFFICIENT +
                computeModelScore(executionState) * MODEL_SCORE_COEFFICIENT
    }

    private fun computePathScore(executionState: ExecutionState): Double =
        executionState.path.groupBy { it }.mapValues { it.value.size - 1 }.values.sum().toDouble()

    private fun computeModelScore(executionState: ExecutionState): Double {
        val status = stateModels.getOrPut(executionState) {
            executionState.solver.check(respectSoft = true)
        } as? UtSolverStatusSAT ?: return INF_SCORE
        val resolver = buildResolver(executionState.memory, status)
        val entryStack = executionState.executionStack.first().localVariableMemory
        val parameters = targets.keys.mapNotNull { entryStack.local(it) }
        if (parameters.size != targets.keys.size) return INF_SCORE

        val afterParameters = resolver.resolveModels(parameters).modelsAfter.parameters
        val models = targets.keys
            .zip(afterParameters)
            .toMap()
            .mapValues { (_, model) ->
                when (model) {
                    is UtAssembleModel -> model.origin!!
                    else -> model
                }
            }

        return computeScore(targets, models)
    }

    private fun computeScore(
        target: Map<LocalVariable, UtModel>,
        current: Map<LocalVariable, UtModel>
    ): Double {
        var currentScore = 0.0
        for ((variable, model) in target) {
            val comparison = when (val computedModel = current[variable]) {
                null -> model.maxScore
                else -> model.score(computedModel)
            }
            currentScore += comparison
        }
        return currentScore
    }

    private val UtModel.maxScore: Double
        get() = when (this) {
            is UtPrimitiveModel -> MAX_SCORE
            is UtAssembleModel -> this.origin?.maxScore ?: MAX_SCORE
            is UtCompositeModel -> {
                var res = 0.0
                for ((_, fieldModel) in this.fields) {
                    res += fieldModel.maxScore
                }
                res
            }
            else -> INF_SCORE
        }

    private fun UtModel.score(other: UtModel): Double = when {
        this.javaClass != other.javaClass -> maxScore
        this is UtPrimitiveModel -> {
            other as UtPrimitiveModel
            maxScore - maxScore / (maxScore + (this - other).abs().toDouble() + EPS)
        }
        this is UtCompositeModel -> {
            other as UtCompositeModel
            var score = 0.0
            for ((field, fieldModel) in this.fields) {
                val otherField = other.fields[field]
                score += when (otherField) {
                    null -> fieldModel.maxScore
                    else -> fieldModel.score(otherField)
                }
            }
            score
        }
        else -> MAX_SCORE.also {
            logger.error { "Unknown ut model" }
        }
    }

    private infix operator fun UtPrimitiveModel.minus(other: UtPrimitiveModel): Number = when (val value = this.value) {
        is Byte -> value - (other.value as Byte)
        is Short -> value - (other.value as Short)
        is Char -> value - (other.value as Char)
        is Int -> value - (other.value as Int)
        is Long -> value - (other.value as Long)
        is Float -> value - (other.value as Float)
        is Double -> value - (other.value as Double)
        is Boolean -> if (value) 1 else 0 - if (other.value as Boolean) 1 else 0
        else -> MAX_SCORE.also {
            logger.error { "Unknown primitive model" }
        }
    }

    private fun Number.abs(): Number = when (this) {
        is Byte -> abs(this.toInt()).toByte()
        is Short -> abs(this.toInt()).toShort()
        is Int -> abs(this)
        is Long -> abs(this)
        is Float -> abs(this)
        is Double -> abs(this)
        else -> 0.also {
            logger.error { "Unknown number" }
        }
    }
}