package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.util.function

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.function.LongFunction

/**
 * Produces values of type [LongFunction].
 *
 * @param <R> return type of produced function
</R> */
class LongFunctionGenerator<R> : org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(LongFunction::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                LongFunction::class.java,
                componentGenerators()[0],
                status
            ), classIdForType(LongFunction::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}