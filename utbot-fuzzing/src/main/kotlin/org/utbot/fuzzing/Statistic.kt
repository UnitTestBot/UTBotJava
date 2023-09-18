package org.utbot.fuzzing

import org.utbot.fuzzing.utils.MissedSeed
import org.utbot.fuzzing.utils.chooseOne
import kotlin.random.Random

/**
 * User class that holds data about current fuzzing running.
 */
interface Statistic<TYPE, RESULT> {
    val startTime: Long
    val totalRuns: Long
    val elapsedTime: Long
    val missedTypes: MissedSeed<TYPE, RESULT>
    val random: Random
    val configuration: Configuration
}

///region Statistic Implementations

/**
 * Interface of statistic object with seeds maintaining logic (seed selection, value storaging, feedbacks counting, etc.)
 */
interface SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>: Statistic<TYPE, RESULT> {
    override var totalRuns: Long
    var lastNewFeedbackIter: Long
    fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>) : MinsetEvent
    fun getSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT>
    fun getMutationsRatings(configuration: Configuration): Map<Mutation<*>, Double>
    fun size() : Int
    fun isEmpty() : Boolean { return size() == 0 }
    fun isNotEmpty() : Boolean { return size() > 0 }
    fun dropMutationsStats()
}


/**
 * Main implementation of [SeedsMaintainingStatistic]. Implements
 * mutations efficiencies evaluating algorithm used for mutation probability tuning (see [MainStatisticImpl.put])
 * and seed selection algorithm based on feedbacks counting (see [MainStatisticImpl.getSeed]).
 */
open class MainStatisticImpl<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>> (
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration,
    override var lastNewFeedbackIter: Long = 0
): SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK> {
    constructor(source: Statistic<TYPE, RESULT>) : this(
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy()
    )

    override val elapsedTime: Long
        get() = System.nanoTime() - startTime

    private val minset: Minset<TYPE, RESULT, FEEDBACK, SingleValueStorage<TYPE, RESULT>> = Minset( { SingleValueStorage() } )

    private var currentValue: Node<TYPE, RESULT>? = null

    val mutationsCounts = mutableMapOf<Mutation<*>, Long>()
    val mutationsSuccessCounts = mutableMapOf<Mutation<*>, Double>()


    override fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>): MinsetEvent {
        val event = minset.put(seed, feedback)

        if (event == MinsetEvent.NEW_FEEDBACK) {
            lastNewFeedbackIter = totalRuns
        }

        seed.result.forEach { result ->
            when (result) {
                is Result.Known<TYPE, RESULT, *> -> {
                    result.lastMutation?.let { mutation ->
                        mutationsCounts[mutation] = mutationsCounts.getOrDefault(mutation, 0) + 1
                        mutationsSuccessCounts[mutation] = mutationsSuccessCounts.getOrDefault(mutation, 0.0) +
                                if (event == MinsetEvent.NEW_FEEDBACK) 1.0 else 0.0
                    }
                }

                is Result.Collection<TYPE, RESULT> -> {
                    result.lastMutation?.let { mutation ->
                        mutationsCounts[mutation] = mutationsCounts.getOrDefault(mutation, 0) + 1
                        mutationsSuccessCounts[mutation] = mutationsSuccessCounts.getOrDefault(mutation, 0.0) +
                                if (event == MinsetEvent.NEW_FEEDBACK) 1.0 else 0.0
                    }
                }

                is Result.Recursive<TYPE, RESULT> -> {
                    result.lastMutation?.let { mutation ->
                        mutationsCounts[mutation] = mutationsCounts.getOrDefault(mutation, 0) + 1
                        mutationsSuccessCounts[mutation] = mutationsSuccessCounts.getOrDefault(mutation, 0.0) +
                                if (event == MinsetEvent.NEW_FEEDBACK) 1.0 else 0.0
                    }
                }

                else -> {}
            }
        }

        return event
    }


    override fun getSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT> {
        if (minset.isEmpty()) error("Call `isNotEmpty` before getting the seed")

        if (configuration.rotateValues && totalRuns % configuration.runsPerValue > 0 && currentValue != null) {
            return currentValue!!
        } else {
            currentValue = minset.getNextSeed(random, configuration.energyFunction)

            if (
                configuration.rotateValues && totalRuns % configuration.runsPerValue == 0L ||
                !configuration.rotateValues && totalRuns % (configuration.globalInvestigationPeriod + configuration.globalExploitationPeriod) == 0L
            ) {
                dropMutationsStats()
            }
        }

        return currentValue!!
    }

    override fun getMutationsRatings(configuration: Configuration): Map<Mutation<*>, Double> {
        val ratings = mutationsCounts.mapValues { (key, _) ->
            configuration.mutationRatingFunction(
                ((mutationsSuccessCounts[key] ?: 0.01) / mutationsSuccessCounts.values.sum())
            )
        }

        return ratings
    }

    override fun size(): Int {
        return minset.size()
    }

    override fun dropMutationsStats() {
        mutationsCounts.clear()
        mutationsSuccessCounts.clear()
    }
}
///endregion

///region Minset

/**
 * Possible answers given by [Minset] after putting a value in it with [Minset.put]:
 * - [NEW_FEEDBACK]: new unique feedback was found;
 * - [NEW_VALUE]: feedback is already in minset, and value stored for it was updated;
 * - [NOTHING_NEW]: feedback is already in minset and the value was not updated either.
 *
 * According to current implementation, [Minset] figures out if feedback is new,
 * but decision between [NEW_VALUE] and [NOTHING_NEW] is making by [ValueStorage].
 */
enum class MinsetEvent { NEW_FEEDBACK, NEW_VALUE, NOTHING_NEW }

/**
 * Storage for feedbacks mapped on the values that led to them.
 */
open class Minset<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>, STORAGE : ValueStorage<TYPE, RESULT>> (
    open val valueStorageGenerator: () -> STORAGE,
    val seeds: LinkedHashMap<FEEDBACK, STORAGE> = linkedMapOf(),
    val count: LinkedHashMap<FEEDBACK, Long> = linkedMapOf()
) {
    fun getNextSeed(random: Random, energyFunction: (feedbackCount: Long, runDuration: Long) -> Double): Node<TYPE, RESULT> {
        val entries = seeds.entries.toList()

        val energy = DoubleArray(size()).also { f ->
            entries.forEachIndexed { index, (key, _) ->
                f[index] = energyFunction(count.getOrDefault(key, 0L), key.runDuration ?: 0)
            }
        }

        return entries[random.chooseOne(energy)].value.next()
    }

    operator fun get(feedback: FEEDBACK): STORAGE? {
        return seeds[feedback]
    }

    open fun put(value: Node<TYPE, RESULT>, feedback: FEEDBACK) : MinsetEvent {
        val result: MinsetEvent

        if (seeds.containsKey(feedback)) {
            result = if( seeds[feedback]!!.put(value, feedback) ) { MinsetEvent.NEW_VALUE } else { MinsetEvent.NOTHING_NEW }
        } else {
            result = MinsetEvent.NEW_FEEDBACK
            seeds[feedback] = valueStorageGenerator.invoke()
            seeds[feedback]!!.put(value, feedback)
        }

        count[feedback] = count.getOrDefault(feedback, 0L) + 1L

        return result
    }

    fun isNotEmpty(): Boolean {
        return seeds.isNotEmpty()
    }

    fun isEmpty(): Boolean {
        return seeds.isEmpty()
    }

    fun size() : Int {
        return seeds.size
    }
}
///endregion

///region Value storages

/**
 * Interface used for [ValueStorage].
 */
interface InfiniteIterator<T> : Iterator<T> {
    override operator fun next(): T
    override operator fun hasNext(): Boolean
}

/**
 * Interface to storage values mapped to feedbacks in [Minset].
 * Now only [SingleValueStorage] implements it, but there might be more complex logic.
 */
interface ValueStorage<TYPE, RESULT> : InfiniteIterator<Node<TYPE, RESULT>> {
    fun put(value: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>) : Boolean
}


/**
 * Base implementation of [ValueStorage] that keeps only one (first or last, according to [strategy])
 * value for each single feedback.
 */
open class SingleValueStorage<TYPE, RESULT> : ValueStorage<TYPE, RESULT> {

    private var storedValue: Node<TYPE, RESULT>? = null

    private var storedWeight: Int = -1
    override fun put(value: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>) : Boolean {

        return if (feedback is WeightedFeedback<*, *>) {

            val result = storedValue == null || storedWeight <= ((feedback as WeightedFeedback<*, *>).weight)

            if (storedValue == null || storedWeight > ((feedback as WeightedFeedback<*, *>).weight)) {
                storedValue = value
                storedWeight = (feedback as WeightedFeedback<*, *>).weight
            }

            result

        } else {
            storedValue =  value
            true
        }
    }

    override fun next(): Node<TYPE, RESULT> {
        if (storedValue == null) {
            error("Next value requested but no value stored")
        } else {
            return storedValue as Node<TYPE, RESULT>
        }
    }

    override fun hasNext(): Boolean {
        return storedValue != null
    }
}

/**
 * [SingleValueStorage] implementation that counts mutations for each feedback stored.
 * Can be used in the future for trying to reach rare feedbacks.
 */
class MutationsCountingSingleValueStorage<TYPE, RESULT> : SingleValueStorage<TYPE, RESULT>() {
    private val knownValueMutationsCount: HashMap<Mutation<*>, Int> = hashMapOf()

    override fun put(value: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>): Boolean {
        value.result.forEach { result ->
            when (result) {
                is Result.Known<*, *, *> -> {
                    result.lastMutation?.let {
                        knownValueMutationsCount[it] = knownValueMutationsCount.getOrDefault(it, 0) + 1
                    }
                }
                else -> {}}
        }
        return super.put(value, feedback)
    }
}

///endregion