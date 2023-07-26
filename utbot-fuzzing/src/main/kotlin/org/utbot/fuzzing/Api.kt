@file:JvmName("FuzzingApi")
package org.utbot.fuzzing

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.utils.MissedSeed
import org.utbot.fuzzing.utils.flipCoin
import org.utbot.fuzzing.utils.transformIfNotEmpty
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private val logger by lazy { KotlinLogging.logger {} }

/**
 * Describes some data to start fuzzing: initial seeds and how to run target program using generated values.
 *
 * @see [org.utbot.fuzzing.demo.AbcFuzzingKt]
 * @see [org.utbot.fuzzing.demo.JavaFuzzing]
 * @see [org.utbot.fuzzing.demo.JsonFuzzingKt]
 */
interface Fuzzing<TYPE, RESULT, DESCRIPTION : Description<TYPE, RESULT>, FEEDBACK : Feedback<TYPE, RESULT>> {

    /**
     * Before producing seeds, this method is called to recognize,
     * whether seeds should be generated especially.
     *
     * [Description.clone] method must be overridden, or it throws an exception if the scope is changed.
     */
    fun enrich(description: DESCRIPTION, type: TYPE, scope: Scope) {}

    /**
     * Generates seeds for a concrete type.
     *
     * If any information except type is required, like parameter index or another,
     * [description] parameter can be used.
     *
     * NB: Fuzzing implementation caches seeds for concrete types to improve performance because
     * usually all seeds are statically defined. In case some dynamic behavior is required use
     * [Feedback.control] to reset caches.
     */
    fun generate(description: DESCRIPTION, type: TYPE): Sequence<Seed<TYPE, RESULT>>

    /**
     * This method is called on every value list generated by fuzzer.
     *
     * Fuzzing combines, randomize and mutates values using the seeds.
     * Then it generates values and runs them with this method. This method should provide some feedback,
     * which is the most important part for a good fuzzing result. [emptyFeedback] can be provided only for test
     * or infinite loops. Consider implementing own implementation of [Feedback] to provide more correct data or
     * use [BaseFeedback] to generate key based feedback. In this case, the key is used to analyze what value should be next.
     *
     * @param description contains user-defined information about the current run. Can be used as a state of the run.
     * @param values current values to process.
     */
    suspend fun handle(description: DESCRIPTION, values: List<RESULT>): FEEDBACK

    /**
     * Starts fuzzing with new description but with copy of [Statistic].
     */
    suspend fun fork(description: DESCRIPTION, statistics: Statistic<TYPE, RESULT>) {
        fuzz(description, BasicSingleValueMinsetStatistic(statistics, SingleValueSelectionStrategy.LAST)
        )
    }

    /**
     * Checks whether the fuzzer should stop.
     */
    suspend fun isCancelled(description: DESCRIPTION, stats: Statistic<TYPE, RESULT>): Boolean {
        return false
    }

    suspend fun beforeIteration(description: DESCRIPTION, statistics: Statistic<TYPE, RESULT>) { }
    suspend fun afterIteration(description: DESCRIPTION, statistics: Statistic<TYPE, RESULT>) { }
}

///region Description
/**
 * Some description of the current fuzzing run. Usually, it contains the name of the target method and its parameter list.
 */
open class Description<TYPE, RESULT>(
    parameters: List<TYPE>
) {
    val parameters: List<TYPE> = parameters.toList()

    open fun clone(scope: Scope): Description<TYPE, RESULT> {
        error("Scope was changed for $this, but method clone is not specified")
    }

    open fun setUp() {}

    open fun updatePerIteration(values: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>) {}

    open fun conclude(statistic: Statistic<TYPE, RESULT>) {}
}

abstract class ReportingDescription<TYPE, RESULT> (
    parameters: List<TYPE>,
    private val reporter: Reporter<TYPE, RESULT>
) : Description<TYPE, RESULT>(parameters) {

    override fun setUp() {
        reporter.setUp(this)
    }

    override fun updatePerIteration(values : Node<TYPE, RESULT>, feedback : Feedback<TYPE, RESULT>) {
        reporter.update(values, feedback)
    }

    override fun conclude(statistic: Statistic<TYPE, RESULT>) {
        reporter.conclude(statistic)
    }
}

class LoggingDescription<TYPE, RESULT> (
    parameters: List<TYPE>,
    path: String
) : ReportingDescription<TYPE, RESULT>(
    parameters,
    LoggingReporter(path = path)
)


///region Reporter
abstract class Reporter<TYPE, RESULT>(
    private val reportPath : String,
) {
    private lateinit var description: Description<TYPE, RESULT>
    fun setUp(description: Description<TYPE, RESULT>) {
        this.description = description
    }
    abstract fun update(values : Node<TYPE, RESULT>, feedback : Feedback<TYPE, RESULT>)
    abstract fun conclude(statistic: Statistic<TYPE, RESULT>)
}


class LoggingReporter<TYPE, RESULT>(
    path: String
) : Reporter<TYPE, RESULT>(path) {

    private val actualPath = if (path.startsWith("~/")) {
        System.getProperty("user.home") + path.drop(1)
    } else {
        path
    }

    private val logFile = File("$actualPath/log")
    private val overviewFile = File("$actualPath/overview.txt")

    init {
        File(actualPath).mkdirs()
        logFile.apply { delete(); createNewFile() }
        overviewFile.apply { delete(); createNewFile() }
    }

    override fun update(values: Node<TYPE, RESULT>, feedback: Feedback<TYPE, RESULT>) {

        fun alignString(obj: Any, length: Int) : String {
            val aligned = obj.toString().replace("\n", "/")
            return if (aligned.length > length) {
                aligned.take((length-1)/2) + ".." + aligned.takeLast((length-1)/2)
            } else {
                aligned + " ".repeat(length - aligned.length)
            }
        }

        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss:SSS"))

        FileOutputStream(logFile, true).bufferedWriter().use {
            it.write("[$time] | ${alignString(values, 53)} | ${alignString(feedback, 30)}\n")
        }
    }

    override fun conclude(statistic: Statistic<TYPE, RESULT>) {
        File(actualPath).mkdirs()
        File("$actualPath/overview.txt").apply{ createNewFile() }.printWriter().use { out ->
            out.println("Total runs: ${statistic.totalRuns}")

            val seconds = statistic.elapsedTime / 1_000_000_000
            val minutes = (seconds % 3600) / 60
            val remainingSeconds = (seconds % 3600) % 60
            val formattedTime = String.format("%02d min %02d.%03d sec", minutes, remainingSeconds, statistic.elapsedTime % 1_000_000)
            out.println("Elapsed time: $formattedTime")
        }
    }
}
///endregion
///endregion



class Scope(
    val parameterIndex: Int,
    val recursionDepth: Int,
    private val properties: MutableMap<ScopeProperty<*>, Any?> = hashMapOf(),
) {
    fun <T> putProperty(param: ScopeProperty<T>, value: T) {
        properties[param] = value
    }

    fun <T> getProperty(param: ScopeProperty<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return properties[param] as? T
    }

    fun isNotEmpty(): Boolean = properties.isNotEmpty()
}

class ScopeProperty<T>(
    val description: String
) {
    fun getValue(scope: Scope): T? {
        return scope.getProperty(this)
    }
}

/**
 * Input value that fuzzing knows how to build and use them.
 */
sealed interface Seed<TYPE, RESULT> {
    /**
     * Simple value is just a concrete value that should be used as is.
     *
     * Any mutation can be provided if it is applicable to this value.
     */
    class Simple<TYPE, RESULT>(val value: RESULT, val mutation: (RESULT, random: Random) -> RESULT = { f, _ -> f }): Seed<TYPE, RESULT>

    /**
     * Known value is a typical value that can be manipulated by fuzzing without knowledge about object structure
     * in concrete language. For example, integer can be represented as a bit vector of n-bits.
     *
     * The list of the known to fuzzing values are:
     *
     * 1. BitVectorValue represents a vector of bits.
     * 2. ...
     */
    class Known<TYPE, RESULT, V : KnownValue<V>>(val value: V, val build: (V) -> RESULT): Seed<TYPE, RESULT>

    /**
     * Recursive value defines an object with typically has a constructor and list of modifications.
     *
     * This task creates a tree of object values.
     */
    class Recursive<TYPE, RESULT>(
        val construct: Routine.Create<TYPE, RESULT>,
        val modify: Sequence<Routine.Call<TYPE, RESULT>> = emptySequence(),
        val empty: Routine.Empty<TYPE, RESULT>
    ) : Seed<TYPE, RESULT>

    /**
     * Collection is a task, that has 2 main options:
     *
     * 1. Construction the collection
     * 2. Modification of the collections that depends on some number of iterations.
     */
    class Collection<TYPE, RESULT>(
        val construct: Routine.Collection<TYPE, RESULT>,
        val modify: Routine.ForEach<TYPE, RESULT>
    ) : Seed<TYPE, RESULT>
}

/**
 * Routine is a task that is used to build a value.
 *
 * There are several types of a routine, which all are generally only functions.
 * These functions accept some data and generate target value.
 */
sealed class Routine<T, R>(val types: List<T>) : Iterable<T> by types {

    /**
     * Creates an empty recursive object.
     */
    class Create<T, R>(
        types: List<T>,
        val builder: (arguments: List<R>) -> R,
    ) : Routine<T, R>(types) {
        operator fun invoke(arguments: List<R>): R = builder(arguments)
    }

    /**
     * Calls routine for a given object.
     */
    class Call<T, R>(
        types: List<T>,
        val callable: (instance: R, arguments: List<R>) -> Unit
    ) : Routine<T, R>(types) {
        operator fun invoke(instance: R, arguments: List<R>) {
            callable(instance, arguments)
        }
    }

    /**
     * Creates a collection of concrete sizes.
     */
    class Collection<T, R>(
        val builder: (size: Int) -> R,
    ) : Routine<T, R>(emptyList()) {
        operator fun invoke(size: Int): R = builder(size)
    }

    /**
     * Is called for a collection with index of iterations.
     */
    class ForEach<T, R>(
        types: List<T>,
        val callable: (instance: R, index: Int, arguments: List<R>) -> Unit
    ) : Routine<T, R>(types) {
        operator fun invoke(instance: R, index: Int, arguments: List<R>) = callable(instance, index, arguments)
    }

    /**
     * Empty routine that generates a concrete value.
     */
    class Empty<T, R>(
        val builder: () -> R,
    ) : Routine<T, R>(emptyList()) {
        operator fun invoke(): R = builder()
    }
}

/**
 * Interface to force [Any.hashCode] and [Any.equals] implementation for [Feedback],
 * because it is used in the map.
 */
interface AsKey {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

/**
 * Language feedback from a concrete execution of the target code.
 */
interface Feedback<TYPE, RESULT> : AsKey {
    /**
     * Controls what fuzzing should do.
     *
     * @see [Control]
     */
    val control: Control
}

/**
 * Base implementation of [Feedback].
 *
 * NB! [VALUE] type must implement [equals] and [hashCode] due to the fact it uses as a key in map.
 * If it doesn't implement those methods, [OutOfMemoryError] is possible.
 */
data class BaseFeedback<VALUE, TYPE, RESULT>(
    val result: VALUE,
    override val control: Control,
) : Feedback<TYPE, RESULT> {
    override fun toString(): String {
        return "$result | $control"
    }
}

interface WeightedFeedback<TYPE, RESULT, WEIGHT : Comparable<WEIGHT>> : Feedback<TYPE, RESULT> {
    val weight: WEIGHT
}

data class BaseWeightedFeedback<VALUE, TYPE, RESULT, WEIGHT : Comparable<WEIGHT>>(
    val result: VALUE,
    override val weight: WEIGHT,
    override val control: Control,
) : WeightedFeedback<TYPE, RESULT, WEIGHT>, Comparable<WeightedFeedback<TYPE, RESULT, WEIGHT>> {
    override fun compareTo(other: WeightedFeedback<TYPE, RESULT, WEIGHT>): Int {
        return weight.compareTo(other.weight)
    }
    override fun toString(): String {
        return "$result with weight $weight | $control"
    }
}


/**
 * Controls fuzzing execution.
 */
enum class Control {
    /**
     * Analyze feedback and continue.
     */
    CONTINUE,

    /**
     * Do not process this feedback and just start the next value generation.
     */
    PASS,

    /**
     * Stop fuzzing.
     */
    STOP,
}

/**
 * Returns empty feedback which is equals to any another empty feedback.
 */
@Suppress("UNCHECKED_CAST")
fun <T, I> emptyFeedback(): Feedback<T, I> = (EmptyFeedback as Feedback<T, I>)

private object EmptyFeedback : Feedback<Nothing, Nothing> {
    override val control: Control
        get() = Control.CONTINUE

    override fun equals(other: Any?): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return 0
    }
}

class NoSeedValueException internal constructor(
    // this type cannot be generalized because Java forbids types for [Throwable].
    val type: Any?
) : Exception() {
    override fun fillInStackTrace(): Throwable {
        return this
    }

    override val message: String
        get() = "No seed candidates generated for type: $type"
}

suspend fun <T, R, D : Description<T, R>, F : Feedback<T, R>> Fuzzing<T, R, D, F>.fuzz(
    description: D,
    random: Random = Random(0),
    configuration: Configuration = Configuration()
) {
    fuzz(description, BasicSingleValueMinsetStatistic(
        random = random, configuration = configuration, seedSelectionStrategy = SingleValueSelectionStrategy.LAST)
    )
}

/**
 * Starts fuzzing for this [Fuzzing] object.
 *
 * This is an entry point for every fuzzing.
 */
private suspend fun <T, R, D : Description<T, R>, F : Feedback<T, R>> Fuzzing<T, R, D, F>.fuzz(
    description: D,
    statistic: SeedsMaintainingStatistic<T, R, F>,
) {
    val random = statistic.random
    val configuration = statistic.configuration
    val fuzzing = this
    val typeCache = hashMapOf<T, List<Seed<T, R>>>()
    val mutationFactory = MutationFactory<T, R>()
    fun fuzzOne(parameters: List<T>): Node<T, R> = fuzz(
        parameters = parameters,
        fuzzing = fuzzing,
        description = description,
        random = random,
        configuration = configuration,
        builder = PassRoutine("Main Routine"),
        state = State(typeCache, statistic.missedTypes),
    )

    description.setUp()

    while (!fuzzing.isCancelled(description, statistic)) {
        beforeIteration(description, statistic)
        val values = if (statistic.isNotEmpty() && random.flipCoin(configuration.probSeedRetrievingInsteadGenerating)) {
            statistic.getRandomSeed(random, configuration).let {
                mutationFactory.mutate(it, random, configuration)
            }
        } else {
            val actualParameters = description.parameters
            // fuzz one value, seems to be bad, when have only a few and simple values
            fuzzOne(actualParameters).let {
                if (random.flipCoin(configuration.probMutationRate)) {
                    mutationFactory.mutate(it, random, configuration)
                } else {
                    it
                }
            }
        }
        afterIteration(description, statistic)

        yield()
        statistic.apply {
            totalRuns++
        }
        check(values.parameters.size == values.result.size) { "Cannot create value for ${values.parameters}" }
        val valuesCache = mutableMapOf<Result<T, R>, R>()
        val result = values.result.map { valuesCache.computeIfAbsent(it) { r -> create(r) } }
        val feedback = fuzzing.handle(description, result)

        description.updatePerIteration(values, feedback)

        if ((statistic.totalRuns % 50).toInt() == 0) {
            println(statistic.totalRuns)
        }


        when (feedback.control) {
            Control.CONTINUE -> {
                statistic.put(random, configuration, feedback, values)
            }
            Control.STOP -> {
                description.conclude(statistic)
                break
            }
            Control.PASS -> {}
        }
    }
}
///region Implementation of the fuzzing and non-public functions.

private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE, RESULT>, FEEDBACK : Feedback<TYPE, RESULT>> fuzz(
    parameters: List<TYPE>,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    description: DESCRIPTION,
    random: Random,
    configuration: Configuration,
    builder: Routine<TYPE, RESULT>,
    state: State<TYPE, RESULT>,
): Node<TYPE, RESULT>  {
    val typeCache = mutableMapOf<TYPE, MutableList<Result<TYPE, RESULT>>>()
    val result = parameters.mapIndexed { index, type ->
        val results = typeCache.computeIfAbsent(type) { mutableListOf() }
        if (results.isNotEmpty() && random.flipCoin(configuration.probReuseGeneratedValueForSameType)) {
            // we need to check cases when one value is passed for different arguments
            results.random(random)
        } else {
            produce(type, fuzzing, description, random, configuration, state.copy {
                parameterIndex = index
            }).also {
                results += it
            }
        }
    }
    // is not inlined to debug values generated for a concrete type
    return Node(result, parameters, builder)
}

private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE, RESULT>, FEEDBACK : Feedback<TYPE, RESULT>> produce(
    type: TYPE,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    description: DESCRIPTION,
    random: Random,
    configuration: Configuration,
    state: State<TYPE, RESULT>,
): Result<TYPE, RESULT> {
    val scope = Scope(state.parameterIndex, state.recursionTreeDepth).apply {
        fuzzing.enrich(description, type, this)
    }
    @Suppress("UNCHECKED_CAST")
    val seeds = when {
        scope.isNotEmpty() -> {
            fuzzing.generate(description.clone(scope) as DESCRIPTION, type).toList()
        }
        else -> state.cache.computeIfAbsent(type) {
            fuzzing.generate(description, it).toList()
        }
    }
    if (seeds.isEmpty()) {
        throw NoSeedValueException(type)
    }
    return seeds.random(random).let {
        when (it) {
            is Seed.Simple<TYPE, RESULT> -> Result.Simple(it.value, it.mutation)
            is Seed.Known<TYPE, RESULT, *> -> it.asResult()
            is Seed.Recursive<TYPE, RESULT> -> reduce(it, fuzzing, description, random, configuration, state)
            is Seed.Collection<TYPE, RESULT> -> reduce(it, fuzzing, description, random, configuration, state)
        }
    }
}

/**
 * reduces [Seed.Collection] type. When `configuration.recursionTreeDepth` limit is reached it creates
 * an empty collection and doesn't do any modification to it.
 */
private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE, RESULT>, FEEDBACK : Feedback<TYPE, RESULT>>  reduce(
    task: Seed.Collection<TYPE, RESULT>,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    description: DESCRIPTION,
    random: Random,
    configuration: Configuration,
    state: State<TYPE, RESULT>,
): Result<TYPE, RESULT> {
    return if (state.recursionTreeDepth > configuration.recursionTreeDepth) {
        Result.Empty { task.construct.builder(0) }
    } else try {
        val iterations = when {
            state.iterations >= 0 && random.flipCoin(configuration.probCreateRectangleCollectionInsteadSawLike) -> state.iterations
            random.flipCoin(configuration.probEmptyCollectionCreation) -> 0
            else -> random.nextInt(1, configuration.collectionIterations + 1)
        }
        Result.Collection(
            construct = fuzz(
                task.construct.types,
                fuzzing,
                description,
                random,
                configuration,
                task.construct,
                state.copy {
                    recursionTreeDepth++
                }
            ),
            modify = if (random.flipCoin(configuration.probCollectionDuplicationInsteadCreateNew)) {
                val result = fuzz(task.modify.types, fuzzing, description, random, configuration, task.modify, state.copy {
                    recursionTreeDepth++
                    this.iterations = iterations
                    parameterIndex = -1
                })
                List(iterations) { result }
            } else {
                (0 until iterations).map {
                    fuzz(task.modify.types, fuzzing, description, random, configuration, task.modify, state.copy {
                        recursionTreeDepth++
                        this.iterations = iterations
                    })
                }
            },
            iterations = iterations
        )
    } catch (nsv: NoSeedValueException) {
        @Suppress("UNCHECKED_CAST")
        state.missedTypes[nsv.type as TYPE] = task
        if (configuration.generateEmptyCollectionsForMissedTypes) {
            Result.Empty { task.construct.builder(0) }
        } else {
            throw nsv
        }
    }
}

/**
 *  reduces [Seed.Recursive] type.  When `configuration.recursionTreeDepth` limit is reached it calls
 *  `Seed.Recursive#empty` routine to create an empty object.
 */
private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE, RESULT>, FEEDBACK : Feedback<TYPE, RESULT>> reduce(
    task: Seed.Recursive<TYPE, RESULT>,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    description: DESCRIPTION,
    random: Random,
    configuration: Configuration,
    state: State<TYPE, RESULT>,
): Result<TYPE, RESULT> {
    return if (state.recursionTreeDepth > configuration.recursionTreeDepth) {
        Result.Empty { task.empty.builder() }
    } else try {
        Result.Recursive(
            construct = fuzz(
                task.construct.types,
                fuzzing,
                description,
                random,
                configuration,
                task.construct,
                state.copy {
                    recursionTreeDepth++
                    iterations = -1
                    parameterIndex = -1
                }
            ),
            modify = task.modify
                .toMutableList()
                .transformIfNotEmpty {
                    shuffle(random)
                    take(configuration.maxNumberOfRecursiveSeedModifications)
                }
                .mapTo(arrayListOf()) { routine ->
                    fuzz(
                        routine.types,
                        fuzzing,
                        description,
                        random,
                        configuration,
                        routine,
                        state.copy {
                            recursionTreeDepth++
                            iterations = -1
                            parameterIndex = -1
                        }
                    )
                }
        )
    } catch (nsv: NoSeedValueException) {
        @Suppress("UNCHECKED_CAST")
        state.missedTypes[nsv.type as TYPE] = task
        if (configuration.generateEmptyRecursiveForMissedTypes) {
            Result.Empty { task.empty.builder() }
        } else {
            throw nsv
        }
    }
}


/**
 * Creates a real result.
 *
 * Fuzzing doesn't use real object because it mutates values by itself.
 */
@Suppress("UNCHECKED_CAST")
private fun <TYPE, R> create(result: Result<TYPE, R>): R = when(result) {
    is Result.Simple<TYPE, R> -> result.result
    is Result.Known<TYPE, R, *> -> (result.build as KnownValue<*>.() -> R)(result.value)
    is Result.Recursive<TYPE, R> -> with(result) {
        val obj: R = when (val c = construct.builder) {
            is Routine.Create<TYPE, R> -> c(construct.result.map { create(it) })
            is Routine.Empty<TYPE, R> -> c()
            else -> error("Undefined create method")
        }
        modify.forEach { func ->
            when (val builder = func.builder) {
                is Routine.Call<TYPE, R> -> builder(obj, func.result.map { create(it) })
                is PassRoutine<TYPE, R> -> logger.warn { "Routine pass: ${builder.description}" }
                else -> error("Undefined object call method ${func.builder}")
            }
        }
        obj
    }
    is Result.Collection<TYPE, R> -> with(result) {
        val collection: R = when (val c = construct.builder) {
            is Routine.Create<TYPE, R> -> c(construct.result.map { create(it) })
            is Routine.Empty<TYPE, R> -> c()
            is Routine.Collection<TYPE, R> -> c(modify.size)
            else -> error("Undefined create method")
        }
        modify.forEachIndexed { index, func ->
            when (val builder = func.builder) {
                is Routine.ForEach<TYPE, R> -> builder(collection, index, func.result.map { create(it) })
                is PassRoutine<TYPE, R> -> logger.warn { "Routine pass: ${builder.description}" }
                else -> error("Undefined collection call method ${func.builder}")
            }
        }
        collection
    }
    is Result.Empty<TYPE, R> -> result.build()
}

/**
 * Empty routine to start a recursion within [fuzz].
 */
private data class PassRoutine<T, R>(val description: String) : Routine<T, R>(emptyList())

/**
 * Internal state for one fuzzing run.
 */
private class State<TYPE, RESULT>(
    val cache: MutableMap<TYPE, List<Seed<TYPE, RESULT>>>,
    val missedTypes: MissedSeed<TYPE, RESULT>,
    val recursionTreeDepth: Int = 1,
    val iterations: Int = -1,
    val parameterIndex: Int = -1,
) {

    fun copy(block: Builder<TYPE, RESULT>.() -> Unit): State<TYPE, RESULT> {
        return Builder(this).apply(block).build()
    }

    class Builder<TYPE, RESULT>(
        state: State<TYPE, RESULT>
    ) {
        var recursionTreeDepth: Int = state.recursionTreeDepth
        var cache: MutableMap<TYPE, List<Seed<TYPE, RESULT>>> = state.cache
        var missedTypes: MissedSeed<TYPE, RESULT> = state.missedTypes
        var iterations: Int = state.iterations
        var parameterIndex: Int = state.parameterIndex

        fun build(): State<TYPE, RESULT> {
            return State(
                cache,
                missedTypes,
                recursionTreeDepth,
                iterations,
                parameterIndex,
            )
        }
    }
}

/**
 * The result of producing real values for the language.
 */
sealed interface Result<TYPE, RESULT> {

    /**
     * Simple result as is.
     */
    class Simple<TYPE, RESULT>(val result: RESULT, val mutation: (RESULT, random: Random) -> RESULT = emptyMutation()) : Result<TYPE, RESULT>

    /**
     * Known value.
     */
    class Known<TYPE, RESULT, V : KnownValue<V>>(val value: V, val build: (V) -> RESULT) : Result<TYPE, RESULT>
    /**
     * A tree of object that has constructor and some modifications.
     */
    class Recursive<TYPE, RESULT>(
        val construct: Node<TYPE, RESULT>,
        val modify: List<Node<TYPE, RESULT>>,
    ) : Result<TYPE, RESULT>

    /**
     * A tree of collection-like structures and their modification.
     */
    class Collection<TYPE, RESULT>(
        val construct: Node<TYPE, RESULT>,
        val modify: List<Node<TYPE, RESULT>>,
        val iterations: Int,
    ) : Result<TYPE, RESULT>

    /**
     * Empty result which just returns a value.
     */
    class Empty<TYPE, RESULT>(
        val build: () -> RESULT
    ) : Result<TYPE, RESULT>
}

/**
 * Temporary object to storage information about partly calculated values tree.
 */
class Node<TYPE, RESULT>(
    val result: List<Result<TYPE, RESULT>>,
    val parameters: List<TYPE>,
    val builder: Routine<TYPE, RESULT>,
) {
    override fun toString() : String {
        return result.map {
            when(it) {
                is Result.Empty -> "_"
                is Result.Simple -> it.result
                is Result.Known<*, *, *> -> it.value.toString()
                is Result.Collection -> it.modify.joinToString(", ", "[", "]")
                is Result.Recursive -> it.modify.joinToString(", ", "{", "}")
            }
        }.joinToString(", ")
    }
}
///endregion


///region Utilities
@Suppress("UNCHECKED_CAST")
private fun <TYPE, RESULT, T : KnownValue<T>> Seed.Known<TYPE, RESULT, *>.asResult(): Result.Known<TYPE, RESULT, T> {
    val value: T = value as T
    return Result.Known(value, build as KnownValue<T>.() -> RESULT)
}
///endregion
