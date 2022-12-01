package org.utbot.engine.greyboxfuzzer.quickcheck.internal.generator

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.Zilch
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness

class ZilchGenerator : Generator(Zilch::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return UtNullModel(objectClassId)// generatorContext.utModelConstructor.construct(Zilch, classIdForType(Zilch::class.java))
    }
}