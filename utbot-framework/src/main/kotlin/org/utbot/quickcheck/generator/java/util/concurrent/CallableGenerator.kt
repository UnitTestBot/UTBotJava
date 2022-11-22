package org.utbot.quickcheck.generator.java.util.concurrent

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.concurrent.Callable

/**
 * Produces values of type `Callable`.
 *
 * @param <V> the type of the values produced by the generated instances
</V> */
class CallableGenerator<V> : ComponentizedGenerator(Callable::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return utModelConstructor.construct(
            makeLambda(
                Callable::class.java,
                componentGenerators()[0],
                status
            ),
            Callable::class.id
        )
    }

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}