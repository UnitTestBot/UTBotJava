package org.utbot.engine.greyboxfuzzer.util

import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.plugin.api.UtModel
import java.util.*

object UtModelGenerator {

    @JvmStatic
    var utModelConstructor = UtModelConstructor(IdentityHashMap<Any, UtModel>())

    fun reset() {
        utModelConstructor = UtModelConstructor(IdentityHashMap<Any, UtModel>())
    }

}