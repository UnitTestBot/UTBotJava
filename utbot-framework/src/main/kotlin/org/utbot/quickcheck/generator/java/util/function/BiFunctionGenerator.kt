package org.utbot.quickcheck.generator.java.util.function

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.function.BiFunction

/**
 * Produces values of type [BiFunction].
 *
 * @param <T> type of first parameter of produced function
 * @param <U> type of second parameter of produced function
 * @param <R> return type of produced function
</R></U></T> */
class BiFunctionGenerator<T, U, R> : ComponentizedGenerator(BiFunction::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                BiFunction::class.java,
                componentGenerators()[2],
                status
            ),
            classIdForType(BiFunction::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 3
    }
}