@file:Suppress("ReplaceRangeStartEndInclusiveWithFirstLast")

package org.utbot.fuzzing

import org.utbot.fuzzing.seeds.*
import org.utbot.fuzzing.utils.chooseOne
import org.utbot.fuzzing.utils.flipCoin
import kotlin.random.Random


class MutationFactory<TYPE, RESULT> {
    fun mutate(
        node: Node<TYPE, RESULT>,
        random: Random,
        configuration: Configuration,
        statistic: SeedsMaintainingStatistic<TYPE, RESULT, *>
    ): Node<TYPE, RESULT> {
        if (node.result.isEmpty()) return node
        val indexOfMutatedResult = random.chooseOne(node.result.map(::rate).toDoubleArray())
        val recursive: NodeMutation<TYPE, RESULT> = NodeMutation { n, r, c ->
            mutate(n, r, c, statistic)
        }
        val mutated = when (val resultToMutate = node.result[indexOfMutatedResult]) {
            is Result.Simple<TYPE, RESULT> -> Result.Simple(
                resultToMutate.mutation(resultToMutate.result, random),
                resultToMutate.mutation
            )
            is Result.Known<TYPE, RESULT, *> -> {
                val mutations = resultToMutate.value.mutations()

                val mutationsEffieciencies =  statistic.getMutationsEfficiencies()

                val mutation = if (
                    configuration.investigationPeriodPerValue > 0 &&
                    statistic.totalRuns % configuration.runsPerValue > configuration.investigationPeriodPerValue &&
                    mutationsEffieciencies.values.sum() >= 0
                ) {
                    mutations[random.chooseOne(mutationsEffieciencies.values.toDoubleArray())]
                } else {
                    mutations.random(random)
                }

                if (mutations.isNotEmpty()) {
                    resultToMutate.mutate(mutation, random, configuration)
                } else {
                    resultToMutate
                }
            }
            is Result.Recursive<TYPE, RESULT> -> {
                when {
                    resultToMutate.modify.isEmpty() || random.flipCoin(configuration.probConstructorMutationInsteadModificationMutation) ->
                        RecursiveMutations.Constructor<TYPE, RESULT>()

                    random.flipCoin(configuration.probShuffleAndCutRecursiveObjectModificationMutation) ->
                        RecursiveMutations.ShuffleAndCutModifications()

                    else -> RecursiveMutations.Mutate()
                }.mutate(resultToMutate, recursive, random, configuration)
            }
            is Result.Collection<TYPE, RESULT> -> if (resultToMutate.modify.isNotEmpty()) {
                when {
                    random.flipCoin(100 - configuration.probCollectionShuffleInsteadResultMutation) ->
                        CollectionMutations.Mutate()

                    else ->
                        CollectionMutations.Shuffle<TYPE, RESULT>()
                }.mutate(resultToMutate, recursive, random, configuration)
            } else {
                resultToMutate
            }
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
        if (!canMutate(result)) {
            return ALMOST_ZERO
        }
        return when (result) {
            is Result.Recursive<TYPE, RESULT> -> if (result.construct.parameters.isEmpty() and result.modify.isEmpty()) ALMOST_ZERO else 0.5
            is Result.Collection<TYPE, RESULT> -> if (result.iterations == 0) return ALMOST_ZERO else 0.7
            is StringValue -> 2.0
            is Result.Known<TYPE, RESULT, *> -> 1.2
            is Result.Simple<TYPE, RESULT> -> 2.0
            is Result.Empty -> ALMOST_ZERO
        }
    }

    private fun <TYPE, RESULT> canMutate(node: Result<TYPE, RESULT>): Boolean {
        return when (node) {
            is Result.Simple<TYPE, RESULT> -> node.mutation === emptyMutation<RESULT>()
            is Result.Known<TYPE, RESULT, *> -> node.value.mutations().isNotEmpty()
            is Result.Recursive<TYPE, RESULT> -> node.modify.isNotEmpty()
            is Result.Collection<TYPE, RESULT> -> node.modify.isNotEmpty() && node.iterations > 0
            is Result.Empty<TYPE, RESULT> -> false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <TYPE, RESULT, T : KnownValue<T>> Result.Known<TYPE, RESULT, *>.mutate(
        mutation: Mutation<T>,
        random: Random,
        configuration: Configuration
    ): Result.Known<TYPE, RESULT, T> {
        val source: T = value as T
        val mutate = mutation.mutate(source, random, configuration)
        return Result.Known(
            mutate,
            build as (T) -> RESULT,
            lastMutation = mutation
        )
    }
}

private const val ALMOST_ZERO = 1E-7
private val IDENTITY_MUTATION: (Any, random: Random) -> Any = { f, _ -> f }

fun <RESULT> emptyMutation(): (RESULT, random: Random) -> RESULT {
    @Suppress("UNCHECKED_CAST")
    return IDENTITY_MUTATION as (RESULT, random: Random) -> RESULT
}

/**
 * Mutations is an object which applies some changes to the source object
 * and then returns a new object (or old one without changes).
 */
fun interface Mutation<T> {
    fun mutate(source: T, random: Random, configuration: Configuration): T
}

sealed class BitVectorMutations : Mutation<BitVectorValue> {

    abstract fun rangeOfMutation(source: BitVectorValue): IntRange

    override fun mutate(source: BitVectorValue, random: Random, configuration: Configuration): BitVectorValue {
        with (rangeOfMutation(source)) {
            val firstBits = random.nextInt(start, endInclusive.coerceAtLeast(1))
            return BitVectorValue(source, this@BitVectorMutations).apply { this[firstBits] = !this[firstBits] }
        }
    }

    object SlightDifferent : BitVectorMutations() {
        override fun rangeOfMutation(source: BitVectorValue) = 0 ..  source.size / 4
    }

    object DifferentWithSameSign : BitVectorMutations() {
        override fun rangeOfMutation(source: BitVectorValue) = source.size / 4 .. source.size
    }

    object ChangeSign : BitVectorMutations() {
        override fun rangeOfMutation(source: BitVectorValue) = source.size - 1 .. source.size
    }
}

sealed interface IEEE754Mutations : Mutation<IEEE754Value> {

    object ChangeSign : IEEE754Mutations {
        override fun mutate(source: IEEE754Value, random: Random, configuration: Configuration): IEEE754Value {
            return IEEE754Value(source, this).apply {
                setRaw(0, !getRaw(0))
            }
        }
    }

    object Mantissa : IEEE754Mutations {
        override fun mutate(source: IEEE754Value, random: Random, configuration: Configuration): IEEE754Value {
            val i = random.nextInt(0, source.mantissaSize)
            return IEEE754Value(source, this).apply {
                setRaw(1 + exponentSize + i, !getRaw(1 + exponentSize + i))
            }
        }
    }

    object Exponent : IEEE754Mutations {
        override fun mutate(source: IEEE754Value, random: Random, configuration: Configuration): IEEE754Value {
            val i = random.nextInt(0, source.exponentSize)
            return IEEE754Value(source, this).apply {
                setRaw(1 + i, !getRaw(1 + i))
            }
        }
    }
}

sealed interface StringMutations : Mutation<StringValue> {

    object AddCharacter : StringMutations {
        override fun mutate(source: StringValue, random: Random, configuration: Configuration): StringValue {
            val value = source.value
            if (value.length >= configuration.maxStringLengthWhenMutated) {
                return source
            }
            val position = random.nextInt(value.length + 1)
            val charToMutate = if (value.isNotEmpty()) {
                value.random(random)
            } else {
                // use any meaningful character from the ascii table
                random.nextInt(33, 127).toChar()
            }
            val newString = buildString {
                append(value.substring(0, position))
                // try to change char to some that is close enough to origin char
                val charTableSpread = 64
                if (random.nextBoolean()) {
                    append(charToMutate - random.nextInt(1, charTableSpread))
                } else {
                    append(charToMutate + random.nextInt(1, charTableSpread))
                }
                append(value.substring(position, value.length))
            }
            return StringValue(newString, lastMutation = this, mutatedFrom = source)
        }
    }

    object RemoveCharacter : StringMutations {
        override fun mutate(source: StringValue, random: Random, configuration: Configuration): StringValue {
            val value = source.value
            val position = random.nextInt(value.length + 1)
            if (position >= value.length) return source
            val toRemove = random.nextInt(value.length)
            val newString = buildString {
                append(value.substring(0, toRemove))
                append(value.substring(toRemove + 1, value.length))
            }
            return StringValue(newString, this)
        }
    }

    object ShuffleCharacters : StringMutations {
        override fun mutate(source: StringValue, random: Random, configuration: Configuration): StringValue {
            return StringValue(
                value = String(source.value.toCharArray().apply { shuffle(random) }),
                lastMutation = this
            )
        }
    }
}

fun interface NodeMutation<TYPE, RESULT> : Mutation<Node<TYPE, RESULT>>

sealed interface CollectionMutations<TYPE, RESULT> : Mutation<Pair<Result.Collection<TYPE, RESULT>, NodeMutation<TYPE, RESULT>>> {

    override fun mutate(
        source: Pair<Result.Collection<TYPE, RESULT>, NodeMutation<TYPE, RESULT>>,
        random: Random,
        configuration: Configuration
    ): Pair<Result.Collection<TYPE, RESULT>, NodeMutation<TYPE, RESULT>> {
        return mutate(source.first, source.second, random, configuration) to source.second
    }

    fun mutate(
        source: Result.Collection<TYPE, RESULT>,
        recursive: NodeMutation<TYPE, RESULT>,
        random: Random,
        configuration: Configuration
    ) : Result.Collection<TYPE, RESULT>

    class Shuffle<TYPE, RESULT> : CollectionMutations<TYPE, RESULT> {
        override fun mutate(
            source: Result.Collection<TYPE, RESULT>,
            recursive: NodeMutation<TYPE, RESULT>,
            random: Random,
            configuration: Configuration
        ): Result.Collection<TYPE, RESULT> {
            return Result.Collection(
                construct = source.construct,
                modify = source.modify.toMutableList().shuffled(random),
                iterations = source.iterations,
                lastMutation = this,
            )
        }
    }

    class Mutate<TYPE, RESULT> : CollectionMutations<TYPE, RESULT> {
        override fun mutate(
            source: Result.Collection<TYPE, RESULT>,
            recursive: NodeMutation<TYPE, RESULT>,
            random: Random,
            configuration: Configuration
        ): Result.Collection<TYPE, RESULT> {
            return Result.Collection(
                construct = source.construct,
                modify = source.modify.toMutableList().apply {
                    val i = random.nextInt(0, source.modify.size)
                    set(i, recursive.mutate(source.modify[i], random, configuration))
                },
                iterations = source.iterations,
                lastMutation = this,
            )
        }
    }
}

sealed interface RecursiveMutations<TYPE, RESULT> : Mutation<Pair<Result.Recursive<TYPE, RESULT>, NodeMutation<TYPE, RESULT>>> {

    override fun mutate(
        source: Pair<Result.Recursive<TYPE, RESULT>, NodeMutation<TYPE, RESULT>>,
        random: Random,
        configuration: Configuration
    ): Pair<Result.Recursive<TYPE, RESULT>, NodeMutation<TYPE, RESULT>> {
        return mutate(source.first, source.second, random, configuration) to source.second
    }

    fun mutate(
        source: Result.Recursive<TYPE, RESULT>,
        recursive: NodeMutation<TYPE, RESULT>,
        random: Random,
        configuration: Configuration
    ) : Result.Recursive<TYPE, RESULT>


    class Constructor<TYPE, RESULT> : RecursiveMutations<TYPE, RESULT> {
        override fun mutate(
            source: Result.Recursive<TYPE, RESULT>,
            recursive: NodeMutation<TYPE, RESULT>,
            random: Random,
            configuration: Configuration
        ): Result.Recursive<TYPE, RESULT> {
            return Result.Recursive(
                construct = recursive.mutate(source.construct,random, configuration),
                modify = source.modify,
                lastMutation = this,
            )
        }
    }

    class ShuffleAndCutModifications<TYPE, RESULT> : RecursiveMutations<TYPE, RESULT> {
        override fun mutate(
            source: Result.Recursive<TYPE, RESULT>,
            recursive: NodeMutation<TYPE, RESULT>,
            random: Random,
            configuration: Configuration
        ): Result.Recursive<TYPE, RESULT> {
            return Result.Recursive(
                construct = source.construct,
                modify = source.modify.shuffled(random).take(random.nextInt(source.modify.size + 1)),
                lastMutation = this,
            )
        }
    }

    class Mutate<TYPE, RESULT> : RecursiveMutations<TYPE, RESULT> {
        override fun mutate(
            source: Result.Recursive<TYPE, RESULT>,
            recursive: NodeMutation<TYPE, RESULT>,
            random: Random,
            configuration: Configuration
        ): Result.Recursive<TYPE, RESULT> {
            return Result.Recursive(
                construct = source.construct,
                modify = source.modify.toMutableList().apply {
                    val i = random.nextInt(0, source.modify.size)
                    set(i, recursive.mutate(source.modify[i], random, configuration))
                },
                lastMutation = this,
            )
        }

    }
}