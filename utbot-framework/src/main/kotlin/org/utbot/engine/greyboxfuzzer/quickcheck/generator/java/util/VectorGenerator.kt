package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.util

import java.util.Vector

/**
 * Produces values of type [Vector].
 */
class VectorGenerator : ListGenerator(Vector::class.java)