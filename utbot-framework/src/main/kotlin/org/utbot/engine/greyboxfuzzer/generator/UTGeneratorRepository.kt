package org.utbot.engine.greyboxfuzzer.generator

import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.quickcheck.internal.generator.GeneratorRepository
import org.utbot.quickcheck.internal.generator.LambdaGenerator
import org.utbot.quickcheck.internal.generator.MarkerInterfaceGenerator
import org.utbot.quickcheck.random.SourceOfRandomness
import org.utbot.engine.logger
import org.utbot.quickcheck.internal.Zilch

class UTGeneratorRepository(random: SourceOfRandomness) : GeneratorRepository(random) {

    override fun generatorFor(parameter: ParameterTypeContext): Generator {
        logger.debug { "TRYING TO GET GENERATOR FOR ${parameter.resolved}" }
        if (parameter.resolved.name == Zilch::class.java.name) return TODO("null")
        val generator = super.generatorFor(parameter)
        if (generator is MarkerInterfaceGenerator) {
            throw IllegalArgumentException(
                "Cannot find generator for " + parameter.name()
                        + " of type " + parameter.type().typeName
            )
        } else if (generator is LambdaGenerator<*>) {
            return TODO("null")
        }
        return generator
    }
}