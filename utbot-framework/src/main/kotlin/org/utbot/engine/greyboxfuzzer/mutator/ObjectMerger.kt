package org.utbot.engine.greyboxfuzzer.mutator

import org.utbot.engine.greyboxfuzzer.generator.FParameter

class ObjectMerger {

    fun mergeObjects(obj1: FParameter, obj2: FParameter) {
        val obj1SubFields = obj1.getAllSubFields()
        val obj2SubFields = obj2.getAllSubFields()
        val randomSubField = obj1SubFields.randomOrNull() ?: return

        return
    }
}