package org.utbot.fuzzing

import org.utbot.fuzzing.utils.Multiset

/**
 * User class that holds data about current fuzzing running.
 *
 * Concrete implementation is passed to the [Fuzzing.update].
 */
interface Statistic<TYPE> {
    val totalRuns: Long
    val elapsedTime: Long
    val missedTypes: Multiset<TYPE>
}