package org.utbot.greyboxfuzzer.generator

import org.utbot.greyboxfuzzer.util.FuzzerIllegalStateException
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import org.utbot.greyboxfuzzer.util.logger
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationState
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import java.lang.reflect.Parameter

object DataGenerator {

    private val generatorRepository = GreyBoxFuzzerGeneratorsAndSettings.generatorRepository

    fun generateUtModel(
        parameterTypeContext: ParameterTypeContext,
        depth: Int = 0,
        generatorContext: GeneratorContext,
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val classId = parameterTypeContext.rawClass.id
        logger.debug { "Trying to generate UtModel of type ${classId.name} 3 times" }
        var generatedInstance: UtModel?
        repeat(3) {
            generatedInstance =
                try {
                    val generator =
                        generatorRepository.getOrProduceGenerator(parameterTypeContext, generatorContext, depth)
                            ?: return@repeat
                    //generator.generatorContext.startCheckpoint()
                    generator.generateImpl(random, status)
                } catch (_: Throwable) {
                    null
                }
            generatedInstance?.let { if (it !is UtNullModel) return it }
        }
        return UtNullModel(classId)
    }

    fun generate(
        parameter: Parameter,
        parameterIndex: Int,
        generatorContext: GeneratorContext,
        random: SourceOfRandomness,
        status: GenerationStatus
    ): FParameter {
        val generator =
            generatorRepository.getOrProduceGenerator(parameter, parameterIndex, generatorContext)
        return generate(generator, parameter, random, status)
    }

    fun generateThis(
        classId: ClassId,
        generatorContext: GeneratorContext,
        random: SourceOfRandomness,
        status: GenerationStatus
    ): NormalMethodThisInstance {
        val generator =
            generatorRepository.getOrProduceGenerator(classId.jClass, generatorContext)
        return generateThis(generator, classId, generatorContext, random, status)
    }

    private fun generateThis(
        generator: Generator?,
        classId: ClassId,
        generatorContext: GeneratorContext,
        random: SourceOfRandomness,
        status: GenerationStatus,
        numberOfTries: Int = 3
    ): NormalMethodThisInstance {
        logger.debug { "Trying to generate this instance of type ${classId.name} $numberOfTries times" }
        generatorRepository.removeGeneratorForObjectClass()
        if (generator == null) {
            throw FuzzerIllegalStateException("Can't find generator for ${classId.name}")
        }
        var generatedValue: UtModel
        repeat(numberOfTries) { iteration ->
            logger.debug { "Try $iteration" }
            try {
                generator.generationState = GenerationState.REGENERATE
                generator.generatorContext.startCheckpoint()
                generatedValue = generator.generateImpl(random, status)
                if (generatedValue is UtNullModel && iteration != numberOfTries - 1) return@repeat
                return NormalMethodThisInstance(
                    generatedValue,
                    generator,
                    classId
                )
            } catch (e: Throwable) {
                logger.error(e) { "Exception while generation :(" }
                return@repeat
            }
        }
        throw FuzzerIllegalStateException("Can't generate for ${classId.name}")
    }

    fun generate(
        generator: Generator?,
        parameter: Parameter,
        random: SourceOfRandomness,
        status: GenerationStatus,
        numberOfTries: Int = 3
    ): FParameter {
        logger.debug { "Trying to generate value for parameter ${parameter.name} of type ${parameter.type} $numberOfTries times" }
        generatorRepository.removeGeneratorForObjectClass()
        val classId = classIdForType(parameter.type)
        if (generator == null) {
            return FParameter(parameter, null, UtNullModel(classId), null, classId, listOf())
        }
        var generatedValue: UtModel?
        repeat(numberOfTries) {
            logger.debug { "Try $it" }
            try {
                generator.generationState = GenerationState.REGENERATE
                generator.generatorContext.startCheckpoint()
                generatedValue = generator.generateImpl(random, status)
                if (generatedValue is UtNullModel) return@repeat
                return FParameter(
                    parameter,
                    null,
                    generatedValue!!,
                    generator,
                    emptyList()
                )
            } catch (e: Throwable) {
                logger.error(e) { "Exception while generation :(" }
                return@repeat
            }
        }
        return FParameter(parameter, null, UtNullModel(classId), generator, classId, listOf())
    }

}