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
import java.util.function.ToLongFunction

/**
 * Produces values of type [ToLongFunction].
 *
 * @param <T> type of parameter of produced function
</T> */
class ToLongFunctionGenerator<T> : org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(ToLongFunction::class.java) {
    private var generator: Generator? = null

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun provide(provided: Generators) {
        super.provide(provided)
        generator = gen()!!.type(Long::class.javaPrimitiveType!!)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                ToLongFunction::class.java, generator!!, status
            ), classIdForType(ToLongFunction::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}