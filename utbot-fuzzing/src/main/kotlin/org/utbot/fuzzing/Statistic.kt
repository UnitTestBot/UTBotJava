package org.utbot.fuzzing

import org.utbot.fuzzing.utils.MissedSeed
import org.utbot.fuzzing.utils.chooseOne
import org.utbot.fuzzing.utils.flipCoin
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

interface SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>: Statistic<TYPE, RESULT> {
    override var totalRuns: Long
    fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>)
    fun getRandomSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT>
    fun isNotEmpty() : Boolean
}

open class BaseStatisticImpl<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>(
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration
) : SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK> {
    constructor(source: Statistic<TYPE, RESULT>) : this(
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy(),
    )

    override val elapsedTime: Long
        get() = System.nanoTime() - startTime
    private val seeds = linkedMapOf<FEEDBACK, Node<TYPE, RESULT>>()
    private val count = linkedMapOf<FEEDBACK, Long>()

    override fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>) {
        if (random.flipCoin(configuration.probUpdateSeedInsteadOfKeepOld)) {
            seeds[feedback] = seed
        } else {
            seeds.putIfAbsent(feedback, seed)
        }
        count[feedback] = count.getOrDefault(feedback, 0L) + 1L
    }

    override fun getRandomSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT> {
        if (seeds.isEmpty()) error("Call `isNotEmpty` before getting the seed")
        val entries = seeds.entries.toList()
        val frequencies = DoubleArray(seeds.size).also { f ->
            entries.forEachIndexed { index, (key, _) ->
                f[index] = configuration.energyFunction(count.getOrDefault(key, 0L))
            }
        }
        val index = random.chooseOne(frequencies)
        return entries[index].value
    }

    override fun isNotEmpty() = seeds.isNotEmpty()
}

open class SingleValueMinsetStatistic<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>, STORAGE : SingleValueStorage<TYPE, RESULT>>(
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration,
    private val generateStorage: () -> STORAGE
) : SeedsMaintainingStatistic<TYPE, RESULT, FEEDBACK> {
    override val elapsedTime: Long
        get() = System.nanoTime() - startTime

    private val minset: Minset<TYPE, RESULT, FEEDBACK, STORAGE> = SingleValueMinset(
        configuration = SingleValueSelectionStrategy.LAST,
        valueStorageGenerator = generateStorage
    )

    constructor(source: Statistic<TYPE, RESULT>, generateStorage: () -> STORAGE) : this(
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy(),
        generateStorage = generateStorage
    )

    override fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>) {
        minset.put(seed, feedback)
    }

    override fun getRandomSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT> {
        if (minset.isEmpty()) error("Call `isNotEmpty` before getting the seed")
        val entries = minset.seeds.entries.toList()
        val frequencies = DoubleArray(minset.seeds.size).also { f ->
            entries.forEachIndexed { index, (key, _) ->
                f[index] = configuration.energyFunction( minset.count.getOrDefault(key, 0L))
            }
        }
        val index = random.chooseOne(frequencies)
        return entries[index].value.next()
    }

    override fun isNotEmpty() = minset.isNotEmpty()
}

class BasicSingleValueMinsetStatistic<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>>(
    override var totalRuns: Long = 0,
    override val startTime: Long = System.nanoTime(),
    override var missedTypes: MissedSeed<TYPE, RESULT> = MissedSeed(),
    override val random: Random,
    override val configuration: Configuration,
    private val seedSelectionStrategy: SingleValueSelectionStrategy
) : SingleValueMinsetStatistic<TYPE, RESULT, FEEDBACK, SingleValueStorage<TYPE, RESULT>>(
    totalRuns, startTime, missedTypes, random, configuration, { SingleValueStorage(seedSelectionStrategy) }
) {
    constructor(source: Statistic<TYPE, RESULT>, seedSelectionStrategy: SingleValueSelectionStrategy) : this (
        totalRuns = source.totalRuns,
        startTime = source.startTime,
        missedTypes = source.missedTypes,
        random = source.random,
        configuration = source.configuration.copy(),
        seedSelectionStrategy = seedSelectionStrategy
    )
}

///endregion

///region Minset
abstract class Minset<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>, STORAGE : ValueStorage<TYPE, RESULT>> (
    open val valueStorageGenerator: () -> STORAGE,
    val seeds: LinkedHashMap<FEEDBACK, STORAGE> = linkedMapOf(),
    val count: LinkedHashMap<FEEDBACK, Long> = linkedMapOf()
) {
    operator fun get(feedback: FEEDBACK): STORAGE? {
        return seeds[feedback]
    }

    fun put(value: Node<TYPE, RESULT>, feedback: FEEDBACK) {
        seeds.getOrPut(feedback) { valueStorageGenerator.invoke() }.put(value, feedback)
        count[feedback] = count.getOrDefault(feedback, 0L) + 1L
    }

    fun isNotEmpty(): Boolean {
        return seeds.isNotEmpty()
    }

    fun isEmpty(): Boolean {
        return seeds.isEmpty()
    }
}

class SingleValueMinset<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>, STORAGE : SingleValueStorage<TYPE, RESULT>> (
    val configuration : SingleValueSelectionStrategy,
    override val valueStorageGenerator: () -> STORAGE,
) : Minset<TYPE, RESULT, FEEDBACK, STORAGE>(valueStorageGenerator)
///endregion

///region Value storages
interface InfiniteIterator<T> : Iterator<T> {
    override operator fun next(): T
    override operator fun hasNext(): Boolean
}

interface ValueStorage<TYPE, RESULT> : InfiniteIterator<Node<TYPE, RESULT>> {
    fun put(value: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>)
}

open class SingleValueStorage<TYPE, RESULT> (
    private val strategy : SingleValueSelectionStrategy
) : ValueStorage<TYPE, RESULT> {

    private var storedValue: Node<TYPE, RESULT>? = null
    override fun put(value: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>) {
        storedValue = when (strategy) {
            SingleValueSelectionStrategy.FIRST -> storedValue ?: value
            SingleValueSelectionStrategy.LAST -> value
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

//@Suppress("UNCHECKED_CAST")
//class SingleDistributableValueStorage<TYPE, RESULT: Comparable<RESULT>>(
//    strategy: SingleValueSelectionStrategy,
//    var distributionTree: DistributionNode<RESULT>
//) : SingleValueStorage<TYPE, RESULT>(strategy) {
//    fun put(value: Node<TYPE, RESULT>, feedback: BaseWeightedFeedback<*, TYPE, RESULT, *>) {
//        super.put(value, feedback)
//        value.result.forEach {
////            distributionTree.put(it)
//            when(it) {
//                is Result.Empty -> {}
//                is Result.Simple -> distributionTree.put(it as RESULT)
//                is Result.Collection -> distributionTree.put(it as RESULT)
//                is Result.Known<*, *, *> -> distributionTree.put(it as RESULT)
//                is Result.Recursive -> distributionTree.put(it as RESULT)
//            }
//        }
//    }
//}

///endregion