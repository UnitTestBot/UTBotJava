package org.utbot.quickcheck.generator.java.util.function

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.Generators
import org.utbot.quickcheck.generator.Lambdas.Companion.makeLambda
import org.utbot.quickcheck.random.SourceOfRandomness
import java.util.function.ToDoubleBiFunction

/**
 * Produces values of type [ToDoubleBiFunction].
 *
 * @param <T> type of first parameter of produced function
 * @param <U> type of second parameter of produced function
</U></T> */
class ToDoubleBiFunctionGenerator<T, U> : ComponentizedGenerator(ToDoubleBiFunction::class.java) {
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