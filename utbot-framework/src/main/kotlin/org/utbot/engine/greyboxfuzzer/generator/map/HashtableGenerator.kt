//package org.utbot.engine.greyboxfuzzer.generator.map
//
//import java.util.*
//
//class HashtableGenerator : MapGenerator<Hashtable<*, *>>(Hashtable::class.java) {
//    override fun okToAdd(key: Any?, value: Any?): Boolean {
//        return key != null && value != null
//    }
//}