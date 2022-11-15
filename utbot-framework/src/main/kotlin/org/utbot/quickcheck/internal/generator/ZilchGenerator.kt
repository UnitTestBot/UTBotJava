package org.utbot.quickcheck.internal.generator

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.internal.Zilch
import org.utbot.quickcheck.random.SourceOfRandomness

class ZilchGenerator : Generator(Zilch::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return UtNullModel(objectClassId)//utModelConstructor.construct(Zilch, classIdForType(Zilch::class.java))
    }
}