package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.logger
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext

class ClassesInstanceGenerator(
    private val clazz: Class<*>,
    private val gctx: GenericsContext,
    private val initialContext: GenericsContext?,
    private val generationMethod: GenerationMethod,
    private val sourceOfRandomness: SourceOfRandomness,
    private val genStatus: GenerationStatus,
    private val generatorContext: GeneratorContext,
    private val depth: Int
): InstanceGenerator {
    override fun generate(): UtModel {
        val typeOfGenerations = when (generationMethod) {
            GenerationMethod.CONSTRUCTOR -> mutableListOf('c')
            GenerationMethod.STATIC -> mutableListOf('s')
            else -> mutableListOf('c', 'c', 's')
        }
        while (typeOfGenerations.isNotEmpty()) {
            val randomTypeOfGeneration = typeOfGenerations.randomOrNull() ?: return UtNullModel(clazz.id)
            logger.debug { "Type of generation: $randomTypeOfGeneration" }
            val generatedInstance =
                try {
                    when (randomTypeOfGeneration) {
                        'c' -> ConstructorBasedInstanceGenerator(
                            clazz,
                            gctx,
                            initialContext,
                            sourceOfRandomness,
                            genStatus,
                            generatorContext,
                            depth
                        ).generate()
                        's' -> StaticsBasedInstanceGenerator(
                            clazz,
                            gctx,
                            sourceOfRandomness,
                            genStatus,
                            generatorContext,
                            depth
                        ).generate()
                        else -> null
                    }
                } catch (_: Throwable) {
                    null
                }
            if (generatedInstance == null || generatedInstance is UtNullModel) {
                typeOfGenerations.remove(randomTypeOfGeneration)
            } else {
                return generatedInstance
            }
        }
        return UtNullModel(clazz.id)
    }
}