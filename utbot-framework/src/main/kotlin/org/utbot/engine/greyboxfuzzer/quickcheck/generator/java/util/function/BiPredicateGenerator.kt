package org.utbot.engine.greyboxfuzzer.quickcheck.generator.java.util.function

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generators
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.function.BiPredicate

/**
 * Produces values of type [BiPredicate].
 *
 * @param <T> type of first parameter of produced predicate
 * @param <U> type of second parameter of produced predicate
</U></T> */
class BiPredicateGenerator<T, U> : org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(BiPredicate::class.java) {
    private var generator: Generator? = null

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun provide(provided: Generators) {
        super.provide(provided)
        generator = gen()!!.type(Boolean::class.javaPrimitiveType!!)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(BiPredicate::class.java, generator!!, status),
            classIdForType(BiPredicate::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 2
    }
}