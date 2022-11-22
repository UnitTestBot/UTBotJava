package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.greyboxfuzzer.generator.GreyBoxFuzzerGenerators
import org.utbot.engine.greyboxfuzzer.generator.getOrProduceGenerator
import org.utbot.engine.greyboxfuzzer.util.getAllTypesFromCastAndInstanceOfInstructions
import org.utbot.engine.greyboxfuzzer.util.getTrue
import org.utbot.engine.greyboxfuzzer.util.toJavaClass
import org.utbot.engine.greyboxfuzzer.util.toSootMethod
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import kotlin.random.Random

class ObjectGenerator(
    private val parameterTypeContext: ParameterTypeContext,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus
) : InstanceGenerator {
    override fun generate(): UtModel {
        val currentSootMethod =
            (parameterTypeContext.generics as? MethodGenericsContext)?.currentMethod()?.toSootMethod()
        val potentialUsefulClasses =
            currentSootMethod?.getAllTypesFromCastAndInstanceOfInstructions()?.mapNotNull { it.toJavaClass() }
        val potentialInterestingObjectReplacement =
            if (potentialUsefulClasses?.isNotEmpty() == true && Random.getTrue(50)) {
                val randomClass = potentialUsefulClasses.random()
                GreyBoxFuzzerGenerators.generatorRepository
                    .getOrProduceGenerator(randomClass)
                    ?.generateImpl(sourceOfRandomness, generationStatus)
            } else null
        potentialInterestingObjectReplacement?.let { return it }
        return GreyBoxFuzzerGenerators.generatorRepository
            .getGenerators()
            .toList()
            .flatMap { it.second }
            .filter { !it.hasComponents() }
            .randomOrNull()
            ?.generateImpl(sourceOfRandomness, generationStatus) ?: UtNullModel(parameterTypeContext.rawClass.id)
    }
}