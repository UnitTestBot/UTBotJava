package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.greyboxfuzzer.quickcheck.generator.Size

/**
 * Base class for generators of [Set]s.
 *
 * */
abstract class SetGenerator constructor(type: Class<*>) : CollectionGenerator(type) {
    override fun configure(size: Size) {
        super.configure(size)
        setDistinct(true)
    }
}