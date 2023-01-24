package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

/**
 * Produces values of type [HashMap].
 */
class HashMapGenerator : MapGenerator(HashMap::class.java)