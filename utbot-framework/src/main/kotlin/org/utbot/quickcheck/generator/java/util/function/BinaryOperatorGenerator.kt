package org.utbot.quickcheck.generator.java.util.function

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.function.BinaryOperator

/**
 * Produces values of type [BinaryOperator].
 *
 * @param <T> parameters type and return type of produced operator
</T> */
class BinaryOperatorGenerator<T> : ComponentizedGenerator(BinaryOperator::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return utModelConstructor.construct(
            makeLambda(
                BinaryOperator::class.java,
                componentGenerators()[0],
                status
            ), classIdForType(BinaryOperator::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}