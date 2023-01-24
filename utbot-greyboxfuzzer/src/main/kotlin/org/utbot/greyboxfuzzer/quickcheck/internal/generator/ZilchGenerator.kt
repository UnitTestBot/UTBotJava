package org.utbot.greyboxfuzzer.quickcheck.internal.generator

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.internal.Zilch
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness

class ZilchGenerator : Generator(Zilch::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return UtNullModel(objectClassId)// generatorContext.utModelConstructor.construct(Zilch, classIdForType(Zilch::class.java))
    }
}