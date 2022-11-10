package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.logger
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext

class ClassesInstanceGenerator(
    private val clazz: Class<*>,
    private val gctx: GenericsContext,
    private val initialContext: GenericsContext?,
    private val generationMethod: GenerationMethod,
    private val sourceOfRandomness: SourceOfRandomness,
    private val genStatus: GenerationStatus,
    private val depth: Int
): InstanceGenerator {
    override fun generate(): UtModel {
        val typeOfGenerations = when (generationMethod) {
            GenerationMethod.CONSTRUCTOR -> mutableListOf('c')
            GenerationMethod.STATIC -> mutableListOf('s')
            else -> mutableListOf('c', 'c', 's')
        }
        while (typeOfGenerations.isNotEmpty()) {
            val randomTypeOfGeneration = typeOfGenerations.randomOrNull() ?: return TODO("null")
            logger.debug { "Type of generation: $randomTypeOfGeneration" }
            val generatedInstance =
                when (randomTypeOfGeneration) {
                    'c' -> ConstructorBasedInstanceGenerator(
                        clazz,
                        gctx,
                        initialContext,
                        sourceOfRandomness,
                        genStatus,
                        depth
                    ).generate()
                    's' -> StaticsBasedInstanceGenerator(
                        clazz,
                        gctx,
                        sourceOfRandomness,
                        genStatus,
                        depth
                    ).generate()
                    else -> null
                }
            if (generatedInstance == null || generatedInstance is UtNullModel) {
                typeOfGenerations.remove(randomTypeOfGeneration)
            } else {
                return generatedInstance
            }
        }
        return TODO("null")
    }
}