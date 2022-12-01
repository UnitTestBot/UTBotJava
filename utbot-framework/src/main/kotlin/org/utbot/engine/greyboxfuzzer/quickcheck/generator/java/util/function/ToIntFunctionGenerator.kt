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
import java.util.function.ToIntFunction

/**
 * Produces values of type [ToIntFunction].
 *
 * @param <T> type of parameter of produced function
</T> */
class ToIntFunctionGenerator<T> : org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator(ToIntFunction::class.java) {
    private var generator: Generator? = null
    override fun provide(provided: Generators) {
        super.provide(provided)
        generator = gen()!!.type(Int::class.javaPrimitiveType!!)
    }

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return generate(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(
            makeLambda(
                ToIntFunction::class.java, generator!!, status
            ), classIdForType(ToIntFunction::class.java)
        )
    }

    override fun numberOfNeededComponents(): Int {
        return 1
    }
}