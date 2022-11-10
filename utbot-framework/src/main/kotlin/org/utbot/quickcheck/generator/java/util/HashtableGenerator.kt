package org.utbot.quickcheck.generator.java.util

import java.util.Hashtable

/**
 * Produces values of type [Hashtable].
 */
class HashtableGenerator : MapGenerator(Hashtable::class.java) {
    override fun okToAdd(key: Any?, value: Any?): Boolean {
        return key != null && value != null
    }
}