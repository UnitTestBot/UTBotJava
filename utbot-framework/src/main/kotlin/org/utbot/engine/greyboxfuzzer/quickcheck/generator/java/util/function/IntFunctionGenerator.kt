package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.util.function

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.function.IntFunction

/**
 * Produces values of type [IntFunction].
 *
 * @param <R> return type of produced function
</R> */
class IntFunctionGenerator<R> : org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(IntFunction::class.java) {

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