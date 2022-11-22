package org.utbot.quickcheck.generator.java.util.function

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.function.Supplier

/**
 * Produces values of type `Supplier`.
 *
 * @param <T> the type of the values produced by the generated instances
</T> */
class SupplierGenerator<T> : ComponentizedGenerator(Supplier::class.java) {

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return utModelConstructor.construct(
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