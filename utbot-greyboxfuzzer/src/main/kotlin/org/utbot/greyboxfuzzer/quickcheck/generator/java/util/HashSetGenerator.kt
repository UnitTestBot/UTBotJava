package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

/**
 * Produces values of type [HashSet].
 */
class HashSetGenerator : SetGenerator(HashSet::class.java)