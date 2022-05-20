package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.BasePathSelector
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.selectors.strategies.StrategyObserver
import kotlin.math.max
import kotlin.random.Random

/**
 * Selects random ExecutionStates according to their weights.
 * If seed is set to null than random is disabled
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L165
 */
abstract class NonUniformRandomSearch(
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int?
) : BasePathSelector(choosingStrategy, stoppingStrategy), StrategyObserver {

    protected class Weight(val cost: Double, val pathLength: Int, val id: Long) : Comparable<Weight> {

        val weight
            get() = 1.0 / max(cost, 1.0)

        override fun compareTo(other: Weight): Int =
            cmp.compare(this, other)

        override fun equals(other: Any?): Boolean =
            cmpIgnoreId.compare(this, other as? Weight) == 0

        override fun hashCode(): Int {
            var result = cost.hashCode()
            result = 31 * result + pathLength
            return result
        }

        companion object {
            /**
             * Comparator that ignores id.
             */
            val cmpIgnoreId = compareBy<Weight> { it.weight }
                .thenBy { it.pathLength }

            private val cmp = cmpIgnoreId.thenBy { -it.id }
        }
    }

    private var uniqueId = 0L
    private val executionQueue = SumAATree<ExecutionState, Weight>(
        compareBy = { it.asWeight },
        weight = { weight }
    )

    private val sumWeights: Double
        get() = executionQueue.sum

    private val randomGen: Random? = seed?.let { Random(seed) }

    override fun update() {
        executionQueue.updateAll()
    }

    override fun offerImpl(state: ExecutionState) {
        executionQueue += state
    }

    override fun queue(): List<Pair<ExecutionState, Double>> {
        return executionQueue.map { Pair(it, it.asWeight.weight) }
    }

    /**
     * Method that peeks random executionStates according to their weights
     * with probability executionState.asWeight.weight / sumWeights
     */
    override fun peekImpl(): ExecutionState? {
        val rand = (randomGen?.nextDouble() ?: 1.0) * sumWeights

        return executionQueue.findLeftest(rand)?.first
    }

    override fun removeImpl(state: ExecutionState): Boolean = executionQueue.remove(state)

    override fun pollImpl(): ExecutionState? =
        peekImpl()?.also { remove(it) }

    override fun isEmpty() =
        executionQueue.isEmpty()

    override fun close() {
        executionQueue.forEach {
            it.close()
        }
    }

    val size: Int get() = executionQueue.size


    /**
     * Cost value of the execution state.
     * The higher cost => the lower probability of selecting that state.
     * Used as first parameter of constructor:
     * Weight(*exState.cost*, exState.pathLength, uniqueId)
     */
    protected abstract val ExecutionState.cost: Double

    /**
     * Build new weight with unique id from ExecutionState.
     */
    private val ExecutionState.asWeight: Weight
        get() = Weight(cost, pathLength, uniqueId++)

}
