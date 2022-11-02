@file:JvmName("FuzzingApi")
package org.utbot.fuzzing

import mu.KotlinLogging
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.utils.chooseOne
import org.utbot.fuzzing.utils.flipCoin
import kotlin.random.Random

private val logger by lazy { KotlinLogging.logger {} }

/**
 * Describes some data to start fuzzing: initial seeds and how to run target program using generated values.
 *
 * @see [org.utbot.fuzzing.demo.AbcFuzzingKt]
 * @see [org.utbot.fuzzing.demo.JavaFuzzing]
 * @see [org.utbot.fuzzing.demo.JsonFuzzingKt]
 */
interface Fuzzing<TYPE, RESULT, DESCRIPTION : Description<TYPE>, FEEDBACK : Feedback<TYPE, RESULT>> {
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
     * Fuzzing combines, randomize and mutates values using the seeds.
     * Then it generates values and runs them with this method. This method should provide some feedback,
     * which is the most important part for good fuzzing result. [emptyFeedback] can be provided only for test
     * or infinite loops. Consider to implement own implementation of [Feedback] to provide more correct data or
     * use [BaseFeedback] to generate key based feedback. In this case, the key is used to analyse what value should be next.
     *
     * @param description contains user-defined information about current run. Can be used as a state of the run.
     * @param values current values to run.
     */
    suspend fun run(description: DESCRIPTION, values: List<RESULT>): FEEDBACK
}

/**
 * Some description of current fuzzing run. Usually, contains name of the target method and its parameter list.
 */
open class Description<TYPE>(
    val parameters: List<TYPE>
)

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
     * List of the known to fuzzing values are:
     *
     * 1. BitVectorValue represents a vector of bits.
     * 2. ...
     */
    class Known<TYPE, RESULT, V : KnownValue>(val value: V, val build: (V) -> RESULT): Seed<TYPE, RESULT>

    /**
     * Recursive value defines an object with typically has a constructor and list of modifications.
     *
     * This task creates a tree of object values.
     */
    open class Recursive<TYPE, RESULT>(
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
    open class Collection<TYPE, RESULT>(
        val construct: Routine.Collection<TYPE, RESULT>,
        val modify: Routine.ForEach<TYPE, RESULT>
    ) : Seed<TYPE, RESULT>
}

/**
 * Routine is a task that is used to build a value.
 *
 * There are several types of a routine, which all are generally only functions.
 * These function accepts some data and generates target value.
 */
sealed class Routine<T, R>(val types: List<T>) : Iterable<T> by types {

    /**
     * Creates an empty recursive object.
     */
    class Create<T, R>(
        types: List<T>,
        val builder: (arguments: List<R>) -> R,
    ) : Routine<T, R>(types), Map<String, Any?> by hashMapOf() {
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
     * Creates a collection of concrete size.
     */
    class Collection<T, R>(
        val builder: (size: Int) -> R,
    ) : Routine<T, R>(emptyList()) {
        operator fun invoke(size: Int): R = builder(size)
    }

    /**
     * Is called for collection with index of iterations.
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
 * If it doesn't implement those method, [OutOfMemoryError] is possible.
 */
data class BaseFeedback<VALUE, TYPE, RESULT>(
    val result: VALUE,
    override val control: Control,
) : Feedback<TYPE, RESULT>

/**
 * Controls fuzzing execution.
 */
enum class Control {
    /**
     * Analyse feedback and continue.
     */
    CONTINUE,

    /**
     * Reset type cache and continue.
     *
     * Current seed and result will be analysed and cached.
     */
    RESET_TYPE_CACHE_AND_CONTINUE,

    /**
     * Do not process this feedback and just start next value generation.
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

/**
 * Starts fuzzing for this [Fuzzing] object.
 *
 * This is an entry point for every fuzzing.
 */
suspend fun <T, R, D : Description<T>, F : Feedback<T, R>> Fuzzing<T, R, D, F>.fuzz(
    description: D,
    random: Random = Random(0),
    configuration: Configuration = Configuration()
) {
    val fuzzing = this
    val typeCache = hashMapOf<T, List<Seed<T, R>>>()
    fun fuzzOne(): Node<T, R> = fuzz(
        parameters = description.parameters,
        fuzzing = fuzzing,
        description = description,
        random = random,
        configuration = configuration,
        builder = PassRoutine("Main Routine"),
        state = State(1, typeCache),
    )
    val dynamicallyGenerated = mutableListOf<Node<T, R>>()
    val seeds = Statistics<T, R, F>()
    run breaking@ {
        sequence {
            while (true) {
                if (dynamicallyGenerated.isNotEmpty()) {
                    yield(dynamicallyGenerated.removeFirst())
                } else {
                    val fuzzOne = fuzzOne()
                    // fuzz one value, seems to be bad, when have only a few and simple values
                    yield(fuzzOne)

                    val randomSeed = seeds.getRandomSeed(random, configuration)
                    if (randomSeed != null) {
                        dynamicallyGenerated += mutate(
                            randomSeed,
                            fuzzing,
                            random,
                            configuration,
                            State(1, typeCache)
                        )
                    }
                }
            }
        }.forEach execution@ { values ->
            val result = values.map { create(it) }
            val feedback = try {
                fuzzing.run(description, result)
            } catch (t: Throwable) {
                logger.error(t) { "Error when running fuzzing with $values" }
                return@execution
            }
            when (feedback.control) {
                Control.CONTINUE -> {
                    seeds.put(random, configuration, feedback, values)
                }
                Control.RESET_TYPE_CACHE_AND_CONTINUE -> {
                    dynamicallyGenerated.clear()
                    typeCache.clear()
                    seeds.put(random, configuration, feedback, values)
                }
                Control.STOP -> { return@breaking }
                Control.PASS -> {}
            }
        }
    }
}


///region Implementation of the fuzzing and non-public functions.

private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE>, FEEDBACK : Feedback<TYPE, RESULT>> fuzz(
    parameters: List<TYPE>,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    description: DESCRIPTION,
    random: Random,
    configuration: Configuration,
    builder: Routine<TYPE, RESULT>,
    state: State<TYPE, RESULT>,
): Node<TYPE, RESULT>  {
    val result = parameters.map { type -> produce(type, fuzzing, description, random, configuration, state) }
    // is not inlined to debug values generated for a concrete type
    return Node(result, parameters, builder)
}

private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE>, FEEDBACK : Feedback<TYPE, RESULT>> produce(
    type: TYPE,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    description: DESCRIPTION,
    random: Random,
    configuration: Configuration,
    state: State<TYPE, RESULT>,
): Result<TYPE, RESULT> {
    val candidates = state.cache.computeIfAbsent(type) { fuzzing.generate(description, type).toList() }.map {
        @Suppress("UNCHECKED_CAST")
        when (it) {
            is Seed.Simple<TYPE, RESULT> -> Result.Simple(it.value, it.mutation)
            is Seed.Known<TYPE, RESULT, out KnownValue> -> Result.Known(it.value, it.build as KnownValue.() -> RESULT)
            is Seed.Recursive<TYPE, RESULT> -> reduce(it, fuzzing, description, random, configuration, state)
            is Seed.Collection<TYPE, RESULT> -> reduce(it, fuzzing, description, random, configuration, state)
        }
    }
    if (candidates.isEmpty()) {
        error("Unknown type: $type")
    }
    return candidates.random(random)
}

/**
 * reduces [Seed.Collection] type. When `configuration.recursionTreeDepth` limit is reached it creates
 * an empty collection and doesn't do any modification to it.
 */
private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE>, FEEDBACK : Feedback<TYPE, RESULT>>  reduce(
    task: Seed.Collection<TYPE, RESULT>,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    description: DESCRIPTION,
    random: Random,
    configuration: Configuration,
    state: State<TYPE, RESULT>,
): Result<TYPE, RESULT> {
    return if (state.recursionTreeDepth > configuration.recursionTreeDepth) {
        Result.Empty { task.construct.builder(0) }
    } else {
        val iterations = if (state.iterations >= 0 && random.flipCoin(configuration.probCreateRectangleCollectionInsteadSawLike)) {
            state.iterations
        } else {
            random.nextInt(0, configuration.collectionIterations + 1)
        }
        Result.Collection(
            construct = fuzz(
                task.construct.types,
                fuzzing,
                description,
                random,
                configuration,
                task.construct,
                State(state.recursionTreeDepth + 1, state.cache, iterations)
            ),
            modify = if (random.flipCoin(configuration.probCollectionMutationInsteadCreateNew)) {
                val result = fuzz(task.modify.types, fuzzing, description, random, configuration, task.modify, State(state.recursionTreeDepth + 1, state.cache, iterations))
                arrayListOf(result).apply {
                    (1 until iterations).forEach { _ ->
                        add(mutate(result, fuzzing, random, configuration, state))
                    }
                }
            } else {
                (0 until iterations).map {
                    fuzz(task.modify.types, fuzzing, description, random, configuration, task.modify, State(state.recursionTreeDepth + 1, state.cache, iterations))
                }
            },
            iterations = iterations
        )
    }
}

/**
 *  reduces [Seed.Recursive] type.  When `configuration.recursionTreeDepth` limit is reached it calls
 *  `Seed.Recursive#empty` routine to create an empty object.
 */
private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE>, FEEDBACK : Feedback<TYPE, RESULT>> reduce(
    task: Seed.Recursive<TYPE, RESULT>,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    description: DESCRIPTION,
    random: Random,
    configuration: Configuration,
    state: State<TYPE, RESULT>,
): Result<TYPE, RESULT> {
    return if (state.recursionTreeDepth > configuration.recursionTreeDepth) {
        Result.Empty { task.empty.builder() }
    } else {
        Result.Recursive(
            construct = fuzz(
                task.construct.types,
                fuzzing,
                description,
                random,
                configuration,
                task.construct,
                State(state.recursionTreeDepth + 1, state.cache)
            ),
            modify = task.modify
                .shuffled()
                .take(configuration.maximumObjectModifications.coerceAtLeast(1))
                .mapTo(arrayListOf()) { routine ->
                    fuzz(
                        routine.types,
                        fuzzing,
                        description,
                        random,
                        configuration,
                        routine,
                        State(state.recursionTreeDepth + 1, state.cache)
                    )
                }
        )
    }
}

/**
 *  Starts mutations of some seeds from the object tree.
 */
@Suppress("UNCHECKED_CAST")
private fun <TYPE, RESULT, DESCRIPTION : Description<TYPE>, FEEDBACK : Feedback<TYPE, RESULT>> mutate(
    node: Node<TYPE, RESULT>,
    fuzzing: Fuzzing<TYPE, RESULT, DESCRIPTION, FEEDBACK>,
    random: Random,
    configuration: Configuration,
    state: State<TYPE, RESULT>,
): Node<TYPE, RESULT> {
    if (node.result.isEmpty()) return node
    val indexOfMutatedResult = random.chooseOne(node.result.map(::rate).toDoubleArray())
    val mutated = when (val resultToMutate = node.result[indexOfMutatedResult]) {
        is Result.Simple<TYPE, RESULT> -> Result.Simple(resultToMutate.mutation(resultToMutate.result, random), resultToMutate.mutation)
        is Result.Known<TYPE, RESULT, out KnownValue> -> {
            val mutations = resultToMutate.value.mutations()
            if (mutations.isNotEmpty()) {
                Result.Known(
                    mutations.random().mutate(resultToMutate.value, random, configuration),
                    resultToMutate.build as KnownValue.() -> RESULT
                )
            } else {
                resultToMutate
            }
        }
        is Result.Recursive<TYPE, RESULT> -> {
            if (resultToMutate.modify.isEmpty() || random.flipCoin(configuration.probConstructorMutationInsteadModificationMutation)) {
                Result.Recursive(
                    construct = mutate(resultToMutate.construct, fuzzing, random, configuration, State(state.recursionTreeDepth + 1, state.cache)),
                    modify = resultToMutate.modify
                )
            } else {
                Result.Recursive(
                    construct = resultToMutate.construct,
                    modify = resultToMutate.modify.toMutableList().apply {
                        val i = random.nextInt(0, resultToMutate.modify.size)
                        set(i, mutate(resultToMutate.modify[i], fuzzing, random, configuration, State(state.recursionTreeDepth + 1, state.cache)))
                    }
                )
            }
        }
        is Result.Collection<TYPE, RESULT> -> Result.Collection(
            construct = resultToMutate.construct,
            modify = resultToMutate.modify.toMutableList().apply {
                if (isNotEmpty()) {
                    if (random.flipCoin(100 - configuration.probCollectionShuffleInsteadResultMutation)) {
                        val i = random.nextInt(0, resultToMutate.modify.size)
                        set(i, mutate(resultToMutate.modify[i], fuzzing, random, configuration, State(state.recursionTreeDepth + 1, state.cache)))
                    } else {
                        shuffle(random)
                    }
                }
            },
            iterations = resultToMutate.iterations
        )
        is Result.Empty -> resultToMutate
    }
    return Node(node.result.toMutableList().apply {
        set(indexOfMutatedResult, mutated)
    }, node.parameters, node.builder)
}

/**
 * Rates somehow the result.
 *
 * For example, fuzzing should not try to mutate some empty structures, like empty collections or objects.
 */
private fun <TYPE, RESULT> rate(result: Result<TYPE, RESULT>): Double {
    return when (result) {
        is Result.Recursive<TYPE, RESULT> -> if (result.construct.parameters.isEmpty() and result.modify.isEmpty()) 1E-7 else 0.5
        is Result.Collection<TYPE, RESULT> -> if (result.iterations == 0) return 0.01 else 0.7
        else -> 1.0
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
    is Result.Known<TYPE, R, out KnownValue> -> (result.build as KnownValue.() -> R)(result.value)
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
    val recursionTreeDepth: Int = 1,
    val cache: MutableMap<TYPE, List<Seed<TYPE, RESULT>>>,
    val iterations: Int = -1
)

/**
 * The result of producing real values for the language.
 */
private sealed interface Result<TYPE, RESULT> {

    /**
     * Simple result as is.
     */
    class Simple<TYPE, RESULT>(val result: RESULT, val mutation: (RESULT, random: Random) -> RESULT = { f, _ -> f }) : Result<TYPE, RESULT>

    /**
     * Known value.
     */
    class Known<TYPE, RESULT, V : KnownValue>(val value: V, val build: (V) -> RESULT) : Result<TYPE, RESULT>
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
private class Node<TYPE, RESULT>(
    val result: List<Result<TYPE, RESULT>>,
    val parameters: List<TYPE>,
    val builder: Routine<TYPE, RESULT>,
) : Iterable<Result<TYPE, RESULT>> by result


private class Statistics<TYPE, RESULT, FEEDBACK : Feedback<TYPE, RESULT>> {
    private val seeds = hashMapOf<FEEDBACK, Node<TYPE, RESULT>>()
    private val count = hashMapOf<FEEDBACK, Long>()

    fun put(random: Random, configuration: Configuration, feedback: FEEDBACK, seed: Node<TYPE, RESULT>) {
        if (random.flipCoin(configuration.probUpdateSeedInsteadOfKeepOld)) {
            seeds[feedback] = seed
        } else {
            seeds.putIfAbsent(feedback, seed)
        }
        count[feedback] = count.getOrDefault(feedback, 0L) + 1L
    }

    fun getRandomSeed(random: Random, configuration: Configuration): Node<TYPE, RESULT>? {
        if (seeds.isEmpty()) return null
        val entries = seeds.entries.toList()
        val frequencies = DoubleArray(seeds.size).also { f ->
            entries.forEachIndexed { index, (key, _) ->
                f[index] = configuration.energyFunction(count.getOrDefault(key, 0L))
            }
        }
        val index = random.chooseOne(frequencies)
        return entries[index].value
    }
}

///endregion