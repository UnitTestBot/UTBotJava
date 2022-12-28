package org.utbot.greyboxfuzzer.quickcheck.generator

/**
 * [Generator]s are fed instances of this interface on each generation
 * so that, if they choose, they can use these instances to influence the
 * results of a generation for a particular property parameter.
 */
interface GenerationStatus {
    /**
     * @return an arbitrary "size" parameter; this value (probabilistically)
     * increases for every successful generation
     */
    fun size(): Int

    /**
     * @return how many attempts have been made to generate a value for a
     * property parameter
     */
    fun attempts(): Int
}