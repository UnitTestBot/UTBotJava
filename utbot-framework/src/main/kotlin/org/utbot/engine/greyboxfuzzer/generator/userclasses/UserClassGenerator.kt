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
import org.utbot.engine.greyboxfuzzer.mutator.Mutator
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.engine.logger
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.quickcheck.generator.GenerationState
import java.lang.reflect.*
import kotlin.random.Random

class UserClassGenerator : ComponentizedGenerator(Any::class.java) {

    var clazz: Class<*>? = null
    var parameterTypeContext: ParameterTypeContext? = null
    var depth = 0
    var generationMethod = GenerationMethod.ANY
    private val mutatedFields = mutableMapOf<Field, Generator>()

    override fun copy(): Generator {
        return UserClassGenerator().also {
            it.clazz = clazz
            it.depth = depth
            it.parameterTypeContext = parameterTypeContext
            it.generationMethod = generationMethod
            it.mutatedFields.putAll(mutatedFields)
        }
    }

    override fun canGenerateForParametersOfTypes(typeParameters: List<TypeParameter<*>>): Boolean {
        return true
    }

    override fun numberOfNeededComponents(): Int {
        return parameterTypeContext?.resolved?.typeParameters?.size ?: 0
    }

    fun generate(random: SourceOfRandomness, status: GenerationStatus, generationMethod: GenerationMethod): UtModel? {
        this.generationMethod = generationMethod
        return generateImpl(random, status)
    }

    private fun regenerate(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        logger.debug { "Trying to generate ${parameterTypeContext!!.resolved}. Current depth depth: $depth" }
        if (depth >= GreyBoxFuzzerGenerators.maxDepthOfGeneration) return TODO("null")
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

    override fun modify(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        val cachedUtModel =
            generatedUtModel as? UtAssembleModel ?: throw FuzzerIllegalStateException("Nothing to modify")
        return if (Random.getTrue(80) && mutatedFields.isNotEmpty()) {
            regenerateField(random, status, cachedUtModel)
        } else {
            setField(cachedUtModel)
        }
    }

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        return modify(random, status)
    }

    private fun regenerateField(
        random: SourceOfRandomness,
        status: GenerationStatus,
        cachedUtModel: UtAssembleModel
    ): UtModel {
        val randomMutatedField = mutatedFields.keys.random()
        val randomMutatedFieldGenerator = mutatedFields[randomMutatedField]!!
        randomMutatedFieldGenerator.generationState = GenerationState.MODIFY
        val newFieldValue = randomMutatedFieldGenerator.generateImpl(random, status)
        val newModification = UtDirectSetFieldModel(cachedUtModel, randomMutatedField.fieldId, newFieldValue)
        return cachedUtModel.addOrReplaceModification(newModification)
    }

    private fun setField(cachedUtModel: UtAssembleModel): UtModel {
        val sootClazz = clazz!!.toSootClass() ?: throw FuzzerIllegalStateException("Can't find soot class")
        val randomField = clazz!!.getAllDeclaredFields().randomOrNull() ?: return cachedUtModel
        val randomFieldDeclaringClass = randomField.declaringClass
        val resolvedJavaType = parameterTypeContext!!.generics.resolveType(parameterTypeContext!!.type())
        val gctx = resolvedJavaType.createGenericsContext(clazz!!)
        if (clazz == randomFieldDeclaringClass) {
            return Mutator.regenerateFieldWithContext(gctx, cachedUtModel, randomField)?.let {
                mutatedFields[randomField] = it.second
                it.first
            } ?: cachedUtModel
        } else {
            val chain = randomFieldDeclaringClass.toSootClass()
                ?.getImplementersOfWithChain()
                ?.filter { it.contains(sootClazz) }
                ?.map { it.dropLastWhile { it != sootClazz } }
                ?.minByOrNull { it.size }
                ?.map { it.toJavaClass() }
            if (chain == null || chain.isEmpty()) {
                return cachedUtModel
            }
            val genericsContext =
                QuickCheckExtensions.buildGenericsContextForInterfaceParent(
                    resolvedJavaType,
                    clazz!!,
                    chain.map { it!! }.reversed().drop(1)
                ) ?: return cachedUtModel
            return Mutator.regenerateFieldWithContext(genericsContext, cachedUtModel, randomField)?.let {
                mutatedFields[randomField] = it.second
                it.first
            } ?: cachedUtModel
        }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return if (generationState == GenerationState.MODIFY) {
            modify(random, status)
        } else {
            regenerate(random, status)
        }.also { generatedUtModel = it }
    }
}
