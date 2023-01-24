package org.utbot.greyboxfuzzer.quickcheck.generator.java.util.function

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.function.DoubleFunction

/**
 * Produces values of type [DoubleFunction].
 *
 * @param <R> return type of produced function
</R> */
class DoubleFunctionGenerator<R> : org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(DoubleFunction::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                DoubleFunction::class.java,
                componentGenerators()[0],
                status
            ), classIdForType(DoubleFunction::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}