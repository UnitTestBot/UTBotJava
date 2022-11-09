package org.utbot.engine.greyboxfuzzer.mutator

import org.javaruntype.type.Types
import org.utbot.engine.greyboxfuzzer.generator.DataGenerator
import org.utbot.engine.greyboxfuzzer.generator.GreyBoxFuzzerGenerators
import org.utbot.engine.greyboxfuzzer.generator.FParameter
import org.utbot.engine.greyboxfuzzer.generator.getOrProduceGenerator
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.engine.logger
import org.utbot.external.api.classIdForType
import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.method
import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.quickcheck.internal.generator.GeneratorRepository
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.random.Random
import java.util.*

object Mutator {

    /**
     * Mean number of mutations to perform in each round.
     */
    private val MEAN_MUTATION_COUNT = 8.0

    /**
     * Mean number of contiguous bytes to mutate in each mutation.
     */
    private val MEAN_MUTATION_SIZE = 4.0 // Bytes

    private fun regenerateRandomParameter(fParameter: FParameter): FParameter? {
        return fParameter
    }

    fun regenerateFields(clazz: Class<*>, classInstance: UtAssembleModel, fieldsToRegenerate: List<Field>): UtModel {
        val parameterTypeContext = ParameterTypeContext.forClass(clazz)
        var resUtModel = classInstance
        for (field in fieldsToRegenerate) {
            resUtModel = setNewFieldValue(field, parameterTypeContext, resUtModel)
        }
        return resUtModel
    }

    private fun setNewFieldValue(
        field: Field,
        parameterTypeContext: ParameterTypeContext,
        clazzInstance: UtAssembleModel
    ): UtAssembleModel {
        field.isAccessible = true
        val oldFieldValue = field.getFieldValue(clazzInstance)
        if (field.hasAtLeastOneOfModifiers(
                Modifier.STATIC,
                Modifier.FINAL
            ) && oldFieldValue != null
        ) return clazzInstance
        val fieldType = parameterTypeContext.generics.resolveFieldType(field)
        logger.debug { "F = $field TYPE = $fieldType OLDVALUE = $oldFieldValue" }
        val parameterTypeContextForResolvedType = ParameterTypeContext(
            field.name,
            field.annotatedType,
            field.declaringClass.name,
            Types.forJavaLangReflectType(fieldType),
            parameterTypeContext.generics
        )
        val newFieldValue = DataGenerator.generate(
            parameterTypeContextForResolvedType,
            GreyBoxFuzzerGenerators.sourceOfRandomness,
            GreyBoxFuzzerGenerators.genStatus
        )
        logger.debug { "NEW FIELD VALUE = $newFieldValue" }
        if (newFieldValue != null) {
            return clazzInstance.addModification(UtDirectSetFieldModel(clazzInstance, field.fieldId, newFieldValue))
        }
        return clazzInstance
    }


    fun mutateParameter(
        fParameter: FParameter
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
                    GreyBoxFuzzerGenerators.generatorRepository.getOrProduceGenerator(resolvedParameterCtx, 0)
                        ?: return fParameter
                DataGenerator.generate(
                    generatorForParameter,
                    parameter,
                    GreyBoxFuzzerGenerators.sourceOfRandomness,
                    GreyBoxFuzzerGenerators.genStatus
                ).utModel
            }
        val callModel = UtExecutableCallModel(fParameter.utModel as UtReferenceModel, randomMethod, parametersForMethodInvocation)
        (originalUtModel as UtAssembleModel).addModification(callModel)
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