package org.utbot.engine.greyboxfuzzer.quickcheck.generator

import org.utbot.framework.concrete.constructors.UtModelConstructor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import java.util.*

data class GeneratorContext(
    val utModelConstructor: UtModelConstructor = UtModelConstructor(IdentityHashMap()),
    val timeoutInMillis: Long = 5000L,
    val constants: MutableMap<ClassId, List<UtModel>> = mutableMapOf()
) {
    var timeOfGenerationStart = 0L
    var timeToFinishGeneration = 0L

    fun startCheckpoint() {
        timeOfGenerationStart = System.currentTimeMillis()
        timeToFinishGeneration = timeOfGenerationStart + timeoutInMillis
    }

    fun checkPoint(): Boolean {
        return System.currentTimeMillis() > timeToFinishGeneration
    }
}