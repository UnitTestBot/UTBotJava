package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.greyboxfuzzer.generator.GreyBoxFuzzerGenerators
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.random.SourceOfRandomness

class ObjectGenerator(
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus
): InstanceGenerator {
    override fun generate(): UtModel? =
        GreyBoxFuzzerGenerators.generatorRepository
            .generators
            .toList()
            .flatMap { it.second }
            .filter { !it.hasComponents() }
            .randomOrNull()
            ?.generate(sourceOfRandomness, generationStatus)
}