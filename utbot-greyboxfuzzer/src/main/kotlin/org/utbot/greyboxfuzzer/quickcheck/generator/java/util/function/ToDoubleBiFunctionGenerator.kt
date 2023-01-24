package org.utbot.greyboxfuzzer.quickcheck.generator.java.util.function

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.Generators
import org.utbot.greyboxfuzzer.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.function.ToDoubleBiFunction

/**
 * Produces values of type [ToDoubleBiFunction].
 *
 * @param <T> type of first parameter of produced function
 * @param <U> type of second parameter of produced function
</U></T> */
class ToDoubleBiFunctionGenerator<T, U> : org.utbot.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(ToDoubleBiFunction::class.java) {
    private var generator: Generator? = null

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun provide(provided: Generators) {
        super.provide(provided)
        generator = gen()!!.type(Double::class.javaPrimitiveType!!)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                ToDoubleBiFunction::class.java, generator!!, status
            ), classIdForType(ToDoubleBiFunction::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 2
    }
}