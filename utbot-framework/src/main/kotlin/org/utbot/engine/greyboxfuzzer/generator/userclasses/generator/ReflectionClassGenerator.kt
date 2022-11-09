package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.internal.ParameterTypeContext

class ReflectionClassGenerator(
    private val parameterTypeContext: ParameterTypeContext
) : InstanceGenerator {
    override fun generate(): UtModel? =
        parameterTypeContext.resolved.typeParameters.randomOrNull()?.type?.componentClass?.let {
            UtModelGenerator.utModelConstructor.construct(it, Class::class.java.id)
        }
}