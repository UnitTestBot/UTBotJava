package org.utbot.engine.greyboxfuzzer.mutator

import org.javaruntype.type.Types
import org.utbot.engine.greyboxfuzzer.generator.*
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.engine.logger
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.method
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationState
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.random.Random

object Mutator {

    fun mutateSeed(seed: Seed, sourceOfRandomness: SourceOfRandomness, genStatus: GenerationStatus): Seed {
        val seedCopy = seed.copy()
        //If seed is empty mutate this
        if (seedCopy.parameters.isEmpty()) return seed

        val randomParameterIndex = Random.nextInt(0, seedCopy.parameters.size)
        val randomParameter = seedCopy.parameters.getOrNull(randomParameterIndex) ?: return seed
        val randomParameterGenerator = randomParameter.generator ?: return seed
        randomParameterGenerator.generationState = GenerationState.MODIFY
        randomParameterGenerator.generatorContext.startCheckpoint()
        val newUtModel = randomParameterGenerator.generateImpl(sourceOfRandomness, genStatus)
        val newFParameter = randomParameter.replaceUtModel(newUtModel)
        return seedCopy.replaceFParameter(randomParameterIndex, newFParameter)
    }

    fun regenerateFields(
        clazz: Class<*>,
        classInstance: UtAssembleModel,
        fieldsToRegenerate: List<Field>,
        generatorContext: GeneratorContext
    ): UtModel {
        val parameterTypeContext = ParameterTypeContext.forClass(clazz)
        val modifications =
            fieldsToRegenerate.mapNotNull { setNewFieldValue(it, parameterTypeContext.generics, classInstance, generatorContext) }
        return classInstance.addModification(modifications)
    }

    fun regenerateFieldsWithContext(
        genericsContext: GenericsContext,
        classInstance: UtAssembleModel,
        fieldsToRegenerate: List<Field>,
        generatorContext: GeneratorContext
    ): UtModel {
        val modifications = fieldsToRegenerate.mapNotNull { setNewFieldValue(it, genericsContext, classInstance, generatorContext) }
        return classInstance.addModification(modifications)
    }

    fun regenerateFieldWithContext(
        genericsContext: GenericsContext,
        classInstance: UtAssembleModel,
        fieldToRegenerate: Field,
        generatorContext: GeneratorContext,
    ): Pair<UtModel, Generator>? =
        setNewFieldValueWithGenerator(
            fieldToRegenerate,
            genericsContext,
            classInstance,
            generatorContext
        )?.let { (generator, modification) ->
            classInstance.addOrReplaceModification(modification) to generator
        }

    private fun setNewFieldValueWithGenerator(
        field: Field,
        genericsContext: GenericsContext,
        clazzInstance: UtAssembleModel,
        generatorContext: GeneratorContext,
    ): Pair<Generator, UtStatementModel>? {
        if (field.hasModifiers(
                Modifier.STATIC,
                Modifier.FINAL
            )
        ) return null
        val fieldType = genericsContext.resolveFieldType(field)
        logger.debug { "F = $field TYPE = $fieldType" }
        val parameterTypeContextForResolvedType = ParameterTypeContext(
            field.name,
            field.annotatedType,
            field.declaringClass.name,
            Types.forJavaLangReflectType(fieldType),
            genericsContext
        )
        val generatorForField =
            GreyBoxFuzzerGenerators.generatorRepository.getOrProduceGenerator(parameterTypeContextForResolvedType, generatorContext, 0)
                ?: return null
        var newFieldValue: UtModel = UtNullModel(parameterTypeContextForResolvedType.rawClass.id)
        for (i in 0 until 3) {
            try {
                generatorForField.generationState = GenerationState.REGENERATE
                generatorForField.generatorContext.startCheckpoint()
                newFieldValue = generatorForField.generateImpl(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
                if (newFieldValue !is UtNullModel) break
            } catch (e: Throwable) {
                continue
            }
        }
        logger.debug { "NEW FIELD VALUE = $newFieldValue" }
        return generatorForField to UtDirectSetFieldModel(clazzInstance, field.fieldId, newFieldValue)
    }

    private fun setNewFieldValue(
        field: Field,
        genericsContext: GenericsContext,
        clazzInstance: UtAssembleModel,
        generatorContext: GeneratorContext,
    ): UtStatementModel? = setNewFieldValueWithGenerator(field, genericsContext, clazzInstance, generatorContext)?.second

    fun mutateThisInstance(
        thisInstance: ThisInstance,
        fieldsToRegenerate: List<Field>,
        generatorContext: GeneratorContext
    ): ThisInstance {
        if (thisInstance !is NormalMethodThisInstance) return thisInstance
        val thisInstanceAsUtModel = thisInstance.utModel as? UtAssembleModel ?: return thisInstance
        val mutationResult =
            regenerateFields(
                thisInstance.classId.jClass,
                thisInstanceAsUtModel,
                fieldsToRegenerate,
                generatorContext
            )
        return NormalMethodThisInstance(mutationResult, thisInstance.generator, thisInstance.classId)
    }

    fun mutateParameter(
        fParameter: FParameter,
        generatorContext: GeneratorContext,
    ): FParameter {
        val originalParameter = fParameter.parameter
        val originalUtModel = fParameter.utModel
//        if (Random.getTrue(100)) {
//            return regenerateRandomParameter(fParameter)
//        }
//        val randomMethod = initialInstance.classId.allMethods
//            .filter { !it.name.startsWith("get") && !it.name.startsWith("to")}
//            .filter { it.classId.name != "java.lang.Object" }
//            .filter { it.parameters.all { !it.name.startsWith("java.util.function") } }
//            .toList()
//            .randomOrNull() ?: return null
        val randomMethod = fParameter.classId.allMethods.toList().randomOrNull() ?: return fParameter
        val parametersForMethodInvocation =
            randomMethod.method.parameters.mapIndexed { index, parameter ->
                val resolvedParameterCtx =
                    originalParameter.resolveParameterTypeAndBuildParameterContext(index, randomMethod.method)
                val generatorForParameter =
                    GreyBoxFuzzerGenerators.generatorRepository.getOrProduceGenerator(resolvedParameterCtx, generatorContext, 0)
                        ?: return fParameter
                DataGenerator.generate(
                    generatorForParameter,
                    parameter,
                    GreyBoxFuzzerGenerators.sourceOfRandomness,
                    GreyBoxFuzzerGenerators.genStatus
                ).utModel
            }
        val callModel =
            UtExecutableCallModel(fParameter.utModel as UtReferenceModel, randomMethod, parametersForMethodInvocation)
        (originalUtModel as? UtAssembleModel)?.addModification(listOf(callModel))
        return FParameter(originalParameter, null, fParameter.utModel, fParameter.generator, fParameter.fields)
    }


//    private fun mutateInput(oldData: Any, sourceOfRandomness: SourceOfRandomness): Any {
//        val castedData = oldData as LongArray
//        print("BEFORE = ")
//        castedData.forEach { print("$it ") }
//        println()
//        // Clone this input to create initial version of new child
//        //val newInput = LinearInput(this)
//        val bos = ByteArrayOutputStream();
//        val oos = ObjectOutputStream(bos);
//        oos.writeObject(oldData);
//        oos.flush();
//        val data = bos.toByteArray()
//        val random = java.util.Random()//sourceOfRandomness.toJDKRandom()
//
//        // Stack a bunch of mutations
//        val numMutations = 3//ZestGuidance.Input.sampleGeometric(random, MEAN_MUTATION_COUNT)
//        println("mutations = $numMutations")
//        //newInput.desc += ",havoc:$numMutations"
//        val setToZero = random.nextDouble() < 0.1 // one out of 10 times
//        for (mutation in 1..numMutations) {
//
//            // Select a random offset and size
//            val offset = random.nextInt(data.size)
//            val mutationSize = ZestGuidance.Input.sampleGeometric(random, MEAN_MUTATION_SIZE)
//
//            // desc += String.format(":%d@%d", mutationSize, idx);
//
//            // Mutate a contiguous set of bytes from offset
//            for (i in offset until offset + mutationSize) {
//                // Don't go past end of list
//                if (i >= data.size) {
//                    break
//                }
//
//                // Otherwise, apply a random mutation
//                val mutatedValue = if (setToZero) 0 else random.nextInt(256)
//                data[i] = mutatedValue.toByte()
//            }
//        }
//        val `in` = ByteArrayInputStream(data)
//        val `is` = ObjectInputStream(`in`)
//        val afterMutationData = `is`.readObject() as LongArray
//        print("AFTER = ")
//        afterMutationData.forEach { print("$it ") }
//        println()
//        return data
//    }

}