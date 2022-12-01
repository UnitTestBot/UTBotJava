package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.util.function

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.function.BinaryOperator

/**
 * Produces values of type [BinaryOperator].
 *
 * @param <T> parameters type and return type of produced operator
</T> */
class BinaryOperatorGenerator<T> : org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(BinaryOperator::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
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