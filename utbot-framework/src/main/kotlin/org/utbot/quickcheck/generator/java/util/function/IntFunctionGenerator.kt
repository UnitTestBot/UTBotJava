package org.utbot.quickcheck.generator.java.util.function

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.function.IntFunction

/**
 * Produces values of type [IntFunction].
 *
 * @param <R> return type of produced function
</R> */
class IntFunctionGenerator<R> : ComponentizedGenerator(IntFunction::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                IntFunction::class.java,
                componentGenerators()[0],
                status
            ), classIdForType(IntFunction::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}