package org.utbot.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.Optional

/**
 * Produces values of type [Optional].
 */
class OptionalGenerator : ComponentizedGenerator(Optional::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val trial = random.nextDouble()
        val classId = Optional::class.id
        if (trial < 0.25) {
            return utModelConstructor.construct(
                Optional.empty<Any>(),
                classId
            )
        }
        val value = componentGenerators().first().generateImpl(random, status)
        val constructorId = methodId(classId, "of", classId, objectClassId)
        val generatedModelId = utModelConstructor.computeUnusedIdAndUpdate()
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