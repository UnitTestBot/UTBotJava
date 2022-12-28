package org.utbot.greyboxfuzzer.quickcheck.generator.java.util.function

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.function.Supplier

/**
 * Produces values of type `Supplier`.
 *
 * @param <T> the type of the values produced by the generated instances
</T> */
class SupplierGenerator<T> : org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(Supplier::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                Supplier::class.java,
                componentGenerators()[0],
                status
            ), classIdForType(Supplier::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}