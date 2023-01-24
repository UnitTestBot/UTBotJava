package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.Optional

/**
 * Produces values of type [Optional].
 */
class OptionalGenerator : org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(Optional::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val trial = random.nextDouble()
        val classId = Optional::class.id
        if (trial < 0.25) {
            return generatorContext.utModelConstructor.construct(
                Optional.empty<Any>(),
                classId
            )
        }
        val value = componentGenerators().first().generateImpl(random, status)
        val constructorId = methodId(classId, "of", classId, objectClassId)
        val generatedModelId =  generatorContext.utModelConstructor.computeUnusedIdAndUpdate()
        return UtAssembleModel(
            generatedModelId,
            classId,
            constructorId.name + "#" + generatedModelId,
            UtExecutableCallModel(null, constructorId, listOf(value)),
        )
    }

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel = generate(random, status)

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}