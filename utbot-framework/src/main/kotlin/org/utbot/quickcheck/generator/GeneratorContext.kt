package org.utbot.quickcheck.generator

import org.utbot.framework.concrete.UtModelConstructor
import java.util.*

data class GeneratorContext(
    val utModelConstructor: UtModelConstructor = UtModelConstructor(IdentityHashMap()),
    val timeoutInMillis: Long = 5000L
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