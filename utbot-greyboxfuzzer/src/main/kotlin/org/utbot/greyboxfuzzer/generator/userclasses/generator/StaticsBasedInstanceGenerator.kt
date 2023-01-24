package org.utbot.greyboxfuzzer.generator.userclasses.generator

import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext
import kotlin.random.Random

open class StaticsBasedInstanceGenerator(
    private val clazz: Class<*>,
    private val gctx: GenericsContext,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus,
    private val generatorContext: GeneratorContext,
    private val depth: Int
) : InstanceGenerator {
    override fun generate(): UtModel {
        val staticMethodBasedGenerator =
            StaticsMethodBasedInstanceGenerator(clazz, gctx, sourceOfRandomness, generationStatus, generatorContext, depth)
        val staticFieldBasedGenerator =
            StaticsFieldBasedInstanceGenerator(clazz, gctx, generatorContext)
        //TODO: repair StaticFieldBasedGenerator
        return if (true) {
            staticMethodBasedGenerator.generate() ?: staticFieldBasedGenerator.generate()
        } else {
            staticFieldBasedGenerator.generate() ?: staticMethodBasedGenerator.generate()
        }
    }
}