package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext
import kotlin.random.Random

open class StaticsBasedInstanceGenerator(
    private val clazz: Class<*>,
    private val gctx: GenericsContext,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus,
    private val depth: Int
) : InstanceGenerator {
    override fun generate(): UtModel? {
        val staticMethodBasedGenerator =
            StaticsMethodBasedInstanceGenerator(clazz, gctx, sourceOfRandomness, generationStatus, depth)
        val staticFieldBasedGenerator =
            StaticsFieldBasedInstanceGenerator(clazz, gctx)
        //TODO: repair StaticFieldBasedGenerator
        return if (true) {
            staticMethodBasedGenerator.generate() ?: staticFieldBasedGenerator.generate()
        } else {
            staticFieldBasedGenerator.generate() ?: staticMethodBasedGenerator.generate()
        }
    }
}