package org.utbot.greyboxfuzzer.generator.userclasses.generator

import org.utbot.greyboxfuzzer.generator.GreyBoxFuzzerGeneratorsAndSettings
import org.utbot.greyboxfuzzer.generator.getOrProduceGenerator
import org.utbot.greyboxfuzzer.util.getAllTypesFromCastAndInstanceOfInstructions
import org.utbot.greyboxfuzzer.util.getTrue
import org.utbot.greyboxfuzzer.util.toSootMethod
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import kotlin.random.Random

class ObjectGenerator(
    private val parameterTypeContext: ParameterTypeContext,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus,
    private val generatorContext: GeneratorContext
) : InstanceGenerator {
    override fun generate(): UtModel {
        val currentSootMethod =
            (parameterTypeContext.generics as? MethodGenericsContext)?.currentMethod()?.toSootMethod()
        val potentialUsefulClasses =
            currentSootMethod?.getAllTypesFromCastAndInstanceOfInstructions()
        val potentialInterestingObjectReplacement =
            if (potentialUsefulClasses?.isNotEmpty() == true && Random.getTrue(70)) {
                val randomClass = potentialUsefulClasses.random()
                val generator = GreyBoxFuzzerGeneratorsAndSettings.generatorRepository
                    .getOrProduceGenerator(randomClass, generatorContext)
                    ?.also { it.generatorContext = generatorContext }
                generator?.generateImpl(sourceOfRandomness, generationStatus)
            } else null
        potentialInterestingObjectReplacement?.let { return it }
        val generator = GreyBoxFuzzerGeneratorsAndSettings.generatorRepository
            .getGenerators()
            .toList()
            .flatMap { it.second }
            .filter { !it.hasComponents() }
            .randomOrNull()
            ?.also { it.generatorContext = generatorContext }
        return generator?.generateImpl(sourceOfRandomness, generationStatus) ?: UtNullModel(parameterTypeContext.rawClass.id)
    }
}