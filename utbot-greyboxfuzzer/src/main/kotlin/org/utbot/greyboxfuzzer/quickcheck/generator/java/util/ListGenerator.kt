package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

/**
 * Base class for generators of [List]s.
 *
 * @param <T> the type of list generated
</T> */
abstract class ListGenerator protected constructor(type: Class<*>) : CollectionGenerator(type)
