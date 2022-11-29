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
import java.util.function.ToDoubleFunction

/**
 * Produces values of type [ToDoubleFunction].
 *
 * @param <T> type of parameter of produced function
</T> */
class ToDoubleFunctionGenerator<T> : ComponentizedGenerator(ToDoubleFunction::class.java) {
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
                ToDoubleFunction::class.java, generator!!, status
            ), classIdForType(ToDoubleFunction::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}