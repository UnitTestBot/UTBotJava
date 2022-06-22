package org.utbot.engine.selectors.strategies

import mu.KotlinLogging
import org.utbot.engine.*
import org.utbot.engine.pc.UtSolverStatus
import org.utbot.engine.pc.UtSolverStatusSAT
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.id
import kotlin.math.abs

interface ScoringStrategy {
    fun score(executionState: ExecutionState): Double

    operator fun get(state: ExecutionState): Double
}

internal object DefaultScoringStrategy : ScoringStrategy {
    override fun score(executionState: ExecutionState): Double = 0.0
    override fun get(state: ExecutionState): Double = 0.0
}


class ModelSynthesisScoringStrategy(
    protected val targets: Map<LocalVariable, UtModel>,
    protected val typeRegistry: TypeRegistry
) : ScoringStrategy {
    private val logger = KotlinLogging.logger("ModelSynthesisScoringStrategy")

    companion object {
        private const val INF_SCORE = Double.MAX_VALUE
        private const val MAX_SCORE = 1.0
        private const val MIN_SCORE = 0.0
        private const val EPS = 1e-5
    }

    // needed for resolver
    private val hierarchy = Hierarchy(typeRegistry)
    private val typeResolver: TypeResolver = TypeResolver(typeRegistry, hierarchy)
    private val softMaxArraySize = 40


    private val stateModels = hashMapOf<ExecutionState, UtSolverStatus>()
    private val scores = hashMapOf<ExecutionState, Double>()

    private fun buildResolver(memory: Memory, holder: UtSolverStatusSAT) =
        Resolver(hierarchy, memory, typeRegistry, typeResolver, holder, "", softMaxArraySize)

    override fun get(state: ExecutionState): Double = scores.getValue(state)

    override fun score(executionState: ExecutionState): Double = scores.getOrPut(executionState) {
        stateModels[executionState] = executionState.solver.check(respectSoft = false)
        return (stateModels[executionState] as? UtSolverStatusSAT)?.let { holder ->
            val resolver = buildResolver(executionState.memory, holder)
            val firstStack = executionState.executionStack.first().localVariableMemory
            val parameters = targets.keys.map { firstStack.local(it) }
            when {
                null in parameters -> INF_SCORE
                else -> {
                    val models =
                        targets.keys.zip(resolver.resolveModels(parameters.filterNotNull()).modelsAfter.parameters)
                            .toMap()
                            .mapValues {
                                val a = it.value
                                if (a is UtAssembleModel) a.origin else a
                            }
                            .mapNotNull {
                                if (it.value == null) null else it.key to it.value!!
                            }
                            .toMap()
                    computeScore(targets, models)
                }
            }
        } ?: INF_SCORE
    }

    private fun computeScore(
        target: Map<LocalVariable, UtModel>,
        current: Map<LocalVariable, UtModel>
    ): Double {
        var currentScore = 0.0
        for ((variable, model) in target) {
            val computedModel = current[variable]
            val comparison = when (computedModel) {
                null -> MAX_SCORE
                else -> model.score(computedModel)
            }
            currentScore += comparison
        }
        return currentScore
    }

    private fun UtModel.score(other: UtModel): Double = when {
        this.javaClass != other.javaClass -> MAX_SCORE
        this is UtPrimitiveModel -> MAX_SCORE - MAX_SCORE / (MAX_SCORE + (this - (other as UtPrimitiveModel)).abs()
            .toDouble() + EPS)
        this is UtCompositeModel -> {
            other as UtCompositeModel
            var score = 0.0
            for ((field, fieldModel) in this.fields) {
                val otherField = other.fields[field]
                score += when (otherField) {
                    null -> MAX_SCORE
                    else -> fieldModel.score(otherField)
                }
            }
            score
        }
        else -> MAX_SCORE.also {
            logger.error { "Unknown ut model" }
        }
    }

    private operator infix fun UtPrimitiveModel.minus(other: UtPrimitiveModel): Number = when (val value = this.value) {
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