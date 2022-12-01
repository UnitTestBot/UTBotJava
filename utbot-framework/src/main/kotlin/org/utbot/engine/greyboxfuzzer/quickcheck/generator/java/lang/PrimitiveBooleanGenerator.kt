package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.lang

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 * Produces values of type `boolean` or [Boolean].
 */
class PrimitiveBooleanGenerator : Generator(listOf(Boolean::class.javaPrimitiveType!!)) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(random.nextBoolean(), booleanClassId)
    }
}