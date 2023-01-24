package org.utbot.greyboxfuzzer.quickcheck.generator.java.util.function

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.function.UnaryOperator

/**
 * Produces values of type [UnaryOperator].
 *
 * @param <T> type of parameter and return type of produced operator
</T> */
class UnaryOperatorGenerator<T> : org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(UnaryOperator::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                UnaryOperator::class.java,
                componentGenerators()[0],
                status
            ), classIdForType(UnaryOperator::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}