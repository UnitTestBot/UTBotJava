//package org.utbot.engine.greyboxfuzzer.generator.map
//
//import java.util.*
//
//class HashMapGenerator : MapGenerator<HashMap<*, *>>(HashMap::class.java) {
//    override fun okToAdd(key: Any?, value: Any?): Boolean {
//        return key != null && value != null
//    }
//}