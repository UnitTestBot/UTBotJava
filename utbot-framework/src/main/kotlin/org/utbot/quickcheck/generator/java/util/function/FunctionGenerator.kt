package org.utbot.quickcheck.generator.java.util.function

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.function.Function

/**
 * Produces values of type [Function].
 *
 * @param <T> type of parameter of produced function
 * @param <R> return type of produced function
</R></T> */
class FunctionGenerator<T, R> : ComponentizedGenerator(Function::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return utModelConstructor.construct(
            makeLambda(
                Function::class.java,
                componentGenerators()[1],
                status
            ), classIdForType(Function::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 2
    }
}