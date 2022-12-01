package org.utbot.engine.greyboxfuzzer.quickcheck.internal.generator

import org.utbot.engine.greyboxfuzzer.generator.GeneratorConfigurator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness

class EnumGenerator(private val enumType: Class<*>) : Generator(Enum::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val values = enumType.enumConstants
        val index = random.nextInt(0, values.size - 1)
        return generatorContext.utModelConstructor.construct(values[index], Enum::class.id)
    }

    override fun copy(): Generator {
        return EnumGenerator(enumType).also {
            it.generatedUtModel = generatedUtModel
            it.generationState = generationState
            it.nestedGenerators = nestedGenerators.map { it.copy() }.toMutableList()
            if (isGeneratorContextInitialized()) {
                it.generatorContext = generatorContext
            }
            GeneratorConfigurator.configureGenerator(it, 95)
        }
    }
}