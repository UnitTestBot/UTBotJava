package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import java.util.LinkedList

/**
 * Produces values of type [LinkedList].
 */
class LinkedListGenerator : ListGenerator(LinkedList::class.java)