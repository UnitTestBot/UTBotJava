package org.utbot.quickcheck.generator.java.lang

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.random.SourceOfRandomness

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