package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import java.util.Stack

/**
 * Produces values of type [Stack].
 */
class StackGenerator : ListGenerator(Stack::class.java)