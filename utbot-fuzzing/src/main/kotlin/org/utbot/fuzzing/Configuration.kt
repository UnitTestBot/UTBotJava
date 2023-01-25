package org.utbot.fuzzing

import kotlin.math.pow

/**
 * Configures fuzzing behaviour. Usually, it is not required to tune anything.
 */
class Configuration(
    /**
     * Fuzzer creates a tree of object for generating values. At some point this recursion should be stopped.
     *
     * To stop recursion [Seed.Recursive.empty] is called to create new values.
     */
    var recursionTreeDepth: Int = 4,

    /**
     * The limit of collection size to create.
     */
    var collectionIterations: Int = 5,

    /**
     * Energy function that is used to choose seeded value.
     */
    var energyFunction: (x: Long) -> Double = { x -> 1 / x.coerceAtLeast(1L).toDouble().pow(2) },

    /**
     * Probability to prefer shuffling collection instead of mutation one value from modification
     */
    var probCollectionShuffleInsteadResultMutation: Int = 75,

    /**
     * Probability of creating shifted array values instead of generating new values for modification.
     */
    var probCollectionMutationInsteadCreateNew: Int = 50,

    /**
     * Probability to prefer change constructor instead of modification.
     */
    var probConstructorMutationInsteadModificationMutation: Int = 90,

    /**
     * Probability to shuffle modification list of the recursive object
     */
    var probShuffleAndCutRecursiveObjectModificationMutation: Int = 10,

    /**
     * Probability to prefer create rectangle collections instead of creating saw-like one.
     */
    var probCreateRectangleCollectionInsteadSawLike: Int = 80,

    /**
     * Probability of updating old seed instead of leaving to the new one when [Feedback] has same key.
     */
    var probUpdateSeedInsteadOfKeepOld: Int = 70,

    /**
     * When mutating StringValue a new string will not exceed this value.
     */
    var maxStringLengthWhenMutated: Int = 64,

    /**
     * Probability of adding a new character when mutating StringValue
     */
    var probStringAddCharacter: Int = 50,

    /**
     * Probability of removing an old character from StringValue when mutating
     */
    var probStringRemoveCharacter: Int = 50,

    /**
     * Probability of reusing same generated value when 2 or more parameters have the same type.
     */
    var probReuseGeneratedValueForSameType: Int = 1,

    /**
     * When true any [Seed.Collection] will not try
     * to generate modification if a current type is already known to fail to generate values.
     */
    var generateEmptyCollectionsForMissedTypes: Boolean = true,

    /**
     * When true any [Seed.Recursive] will not try
     * to generate a recursive object, but will use [Seed.Recursive.empty] instead.
     */
    var generateEmptyRecursiveForMissedTypes: Boolean = true,
)