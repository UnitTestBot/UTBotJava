package org.utbot.fuzzing

import kotlin.math.pow

/**
 * Configures fuzzing behaviour. Usually, it is not required to tune anything.
 */
data class Configuration(

    var tuneKnownValueMutations: Boolean = true,
    var tuneCollectionMutations: Boolean = true,
    var tuneRecursiveMutations: Boolean = false,

    /**
     * When true, fuzzer selects one value and runs it for [runsPerValue] times, first [investigationPeriodPerValue] of which
     * are investigational. When false, fuzzer investigates mutations efficiencies for [globalInvestigationPeriod] runs and
     * tunes it for next [globalExploitationPeriod] runs, then drops statistics and starts over.
     */
    var rotateValues: Boolean = false,

    /**
     * Number of iterations before mutations probabilities correction when [rotateValues] is false.
     */
    var globalInvestigationPeriod: Long = 1000,

    /**
     * Number of iterations before dropping statistics when [rotateValues] is false.
     */
    var globalExploitationPeriod: Long = 1500,

    /**
     * Configuration used by minset to accumulate and use statistics.
     */
    var minsetConfiguration: MinsetConfiguration = MinsetConfiguration(),

    /**
     * Number of continuous iterations for each value when [rotateValues] is true.
     */
    var runsPerValue: Long = 50,

    /**
     * Number of iterations before mutations probabilities correction when [rotateValues] is true.
     */
    var investigationPeriodPerValue: Int = 25,

    /**
     * Choose between already generated values and new generation of values.
     */
    var probSeedRetrievingInsteadGenerating: Int = 70,

    /**
     * Choose between generation and mutation.
     */
    var probMutationRate: Int = 99,

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
    var probCollectionDuplicationInsteadCreateNew: Int = 10,

    /**
     * Probability of creating empty collections
     */
    var probEmptyCollectionCreation: Int = 1,

    /**
     * Probability to prefer change constructor instead of modification.
     */
    var probConstructorMutationInsteadModificationMutation: Int = 30,

    /**
     * Probability to a shuffle modification list of the recursive object
     */
    var probShuffleAndCutRecursiveObjectModificationMutation: Int = 30,

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
    var maxStringLengthWhenMutated: Int = 128,

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

    /**
     * Limits maximum number of recursive seed modifications
     */
    var maxNumberOfRecursiveSeedModifications: Int = 10,
)

/**
 * Configuration used by minset to accumulate and use statistics.
 */
data class MinsetConfiguration (
    var rewardMultiplier: Double = 1.0,
    var rewardWeight: Double = 0.0,
    var penaltyMultiplier: Double = 1.0,
    var penaltyWeight: Double = 0.0
)

val carrot = MinsetConfiguration(
    rewardMultiplier = 1.1,
    rewardWeight = 1.25,
    penaltyMultiplier = 1.0,
    penaltyWeight = 0.0
)

val stick = MinsetConfiguration(
    rewardMultiplier = 0.0,
    rewardWeight = 0.0,
    penaltyMultiplier = 1.1,
    penaltyWeight = 1.0
)