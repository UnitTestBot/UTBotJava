@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.utbot.engine.greyboxfuzzer.generator.userclasses

import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.quickcheck.random.SourceOfRandomness
import org.javaruntype.type.TypeParameter
import org.utbot.engine.greyboxfuzzer.generator.*
import org.utbot.engine.greyboxfuzzer.generator.userclasses.generator.*
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.engine.logger
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.id
import java.lang.reflect.*

class UserClassGenerator : ComponentizedGenerator<Any>(Any::class.java) {

    var clazz: Class<*>? = null
    var parameterTypeContext: ParameterTypeContext? = null
    var depth = 0
    var generationMethod = GenerationMethod.ANY

    override fun copy(): Generator<Any> {
        return UserClassGenerator().also {
            it.clazz = clazz
            it.depth = depth
            it.parameterTypeContext = parameterTypeContext
        }
    }

    override fun canGenerateForParametersOfTypes(typeParameters: MutableList<TypeParameter<*>>?): Boolean {
        return true
    }

    override fun numberOfNeededComponents(): Int {
        return parameterTypeContext?.resolved?.typeParameters?.size ?: 0
    }

    fun generate(random: SourceOfRandomness, status: GenerationStatus, generationMethod: GenerationMethod): UtModel? {
        this.generationMethod = generationMethod
        return generate(random, status)
    }

    override fun generate(random: SourceOfRandomness, status: GenerationStatus): UtModel? {
        logger.debug { "Trying to generate ${parameterTypeContext!!.resolved}. Current depth depth: $depth" }
        if (depth >= GreyBoxFuzzerGenerators.maxDepthOfGeneration) return null
        val immutableClazz = clazz!!
        if (immutableClazz == Any::class.java) return ObjectGenerator(random, status).generate()
        if (immutableClazz == Class::class.java) return ReflectionClassGenerator(parameterTypeContext!!).generate()
        //TODO! generate inner classes instances
        if (immutableClazz.declaringClass != null && !immutableClazz.hasModifiers(Modifier.STATIC)) {
            return UtNullModel(immutableClazz.id)
        }
        val resolvedJavaType = parameterTypeContext!!.generics.resolveType(parameterTypeContext!!.type())
        val gctx = resolvedJavaType.createGenericsContext(immutableClazz)
        if (!immutableClazz.canBeInstantiated()) {
            return InterfaceImplementationsInstanceGenerator(
                resolvedJavaType,
                gctx,
                GreyBoxFuzzerGenerators.sourceOfRandomness,
                GreyBoxFuzzerGenerators.genStatus,
                depth
            ).generate()
        }
        return ClassesInstanceGenerator(
            clazz!!,
            gctx,
            parameterTypeContext!!.generics,
            generationMethod,
            GreyBoxFuzzerGenerators.sourceOfRandomness,
            GreyBoxFuzzerGenerators.genStatus,
            depth
        ).generate()
    }
}
