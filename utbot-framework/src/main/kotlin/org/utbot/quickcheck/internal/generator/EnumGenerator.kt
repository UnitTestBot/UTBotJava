package org.utbot.quickcheck.internal.generator

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.random.SourceOfRandomness

class EnumGenerator(private val enumType: Class<*>) : Generator(Enum::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val values = enumType.enumConstants
        val index = random.nextInt(0, values.size - 1)
        return utModelConstructor.construct(values[index], Enum::class.id)
    }
}