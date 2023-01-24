package org.utbot.greyboxfuzzer.quickcheck.generator

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.util.FuzzerUtModelConstructor

data class GeneratorContext(
    val utModelConstructor: FuzzerUtModelConstructor,
    val constants: Map<ClassId, List<UtModel>>,
    val timeoutInMillis: Long = 5000L,
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