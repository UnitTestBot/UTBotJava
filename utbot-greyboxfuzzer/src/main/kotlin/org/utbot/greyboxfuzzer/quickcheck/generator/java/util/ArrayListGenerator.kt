package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

/**
 * Produces values of type [ArrayList].
 */
class ArrayListGenerator : ListGenerator(ArrayList::class.java)