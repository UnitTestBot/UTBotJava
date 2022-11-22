package org.utbot.engine.greyboxfuzzer.generator

import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.quickcheck.random.SourceOfRandomness
import org.utbot.engine.logger
import org.utbot.quickcheck.internal.Zilch
import org.utbot.quickcheck.internal.generator.*

class UTGeneratorRepository(random: SourceOfRandomness) : GeneratorRepository(random) {

    override fun generatorFor(parameter: ParameterTypeContext): Generator {
        logger.debug { "TRYING TO GET GENERATOR FOR ${parameter.resolved}" }
        if (parameter.resolved.name == Zilch::class.java.name) return ZilchGenerator()
        val generator = super.generatorFor(parameter)
        if (generator is MarkerInterfaceGenerator) {
            throw IllegalArgumentException(
                "Cannot find generator for " + parameter.name()
                        + " of type " + parameter.type().typeName
            )
        }
        return generator
    }

//    override fun generatorsFor(parameter: ParameterTypeContext) =
//        super.generatorsFor(parameter)//.onEach { GeneratorConfigurator.configureGenerator(it, 85) }
//
//    override fun generatorForArrayType(parameter: ParameterTypeContext) =
//         super.generatorForArrayType(parameter)//.also { GeneratorConfigurator.configureGenerator(it, 85) }


}