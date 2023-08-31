package org.utbot.fuzzing

import kotlin.math.pow

/**
 * Configures fuzzing behaviour. Usually, it is not required to tune anything.
 */
data class Configuration(

    var tuneKnownValueMutations: Boolean = true,
    var tuneCollectionMutations: Boolean = true,
    var tuneRecursiveMutations: Boolean = true,

    /**
     * When true, fuzzer selects one value and runs it for [runsPerValue] times, first [investigationPeriodPerValue] of which
     * are investigational. When false, fuzzer investigates mutations efficiencies for [globalInvestigationPeriod] runs and
     * tunes it for next [globalExploitationPeriod] runs, then drops statistics and starts over.
     */
    var rotateValues: Boolean = false,

    /**
     * Number of iterations before mutations probabilities correction when [rotateValues] is false.
     */
    var globalInvestigationPeriod: Long = 100,

    /**
     * Number of iterations before dropping statistics when [rotateValues] is false.
     */
    var globalExploitationPeriod: Long = 900,

    /**
     * Number of continuous iterations for each value when [rotateValues] is true.
     */
    var runsPerValue: Long = 500,

    /**
     * Number of iterations before mutations probabilities correction when [rotateValues] is true.
     */
    var investigationPeriodPerValue: Int = 250,

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
    var energyFunction: (feedbackCount: Long, runDuration: Long) -> Double = {
        // x, _ -> 1 / x.coerceAtLeast(1L).toDouble().pow(2)
        feedbackCount, runDuration -> 1000 /
            feedbackCount.coerceAtLeast(1L).toDouble().pow(2) /
            runDuration.coerceAtLeast(1L).toDouble().pow(2)
    },

    /**
     * Rating function that is used to manipulate mutations probabilities while tuning.
     */
    var mutationRatingFunction: (successProbability: Double) -> Double = { successProbability -> successProbability.pow(2) },

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

