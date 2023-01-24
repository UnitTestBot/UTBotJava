package org.utbot.greyboxfuzzer.mutator

import org.javaruntype.type.Types
import org.utbot.greyboxfuzzer.generator.*
import org.utbot.greyboxfuzzer.util.*
import org.utbot.greyboxfuzzer.util.logger
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationState
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.context.GenericsContext
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.random.Random

class Mutator {

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

    private fun regenerateFields(
        clazz: Class<*>,
        classInstance: UtAssembleModel,
        fieldsToRegenerate: List<Field>,
        generatorContext: GeneratorContext
    ): UtModel {
        val modifications = mutableListOf<UtStatementModel>()
        try {
            val parameterTypeContext = ParameterTypeContext.forClass(clazz)
            fieldsToRegenerate.mapNotNull {
                setNewFieldValue(
                    it,
                    parameterTypeContext.generics,
                    classInstance,
                    generatorContext
                )
            }.forEach { modifications.add(it) }
        } catch (e: Throwable) {
            logger.debug { "Exception while mutation: ${e.stackTrace.joinToString("\n")}" }
        }
        return classInstance.addModification(modifications)
    }

    fun regenerateFieldsWithContext(
        genericsContext: GenericsContext,
        classInstance: UtAssembleModel,
        fieldsToRegenerate: List<Field>,
        generatorContext: GeneratorContext
    ): UtModel {
        val modifications =
            fieldsToRegenerate.mapNotNull { setNewFieldValue(it, genericsContext, classInstance, generatorContext) }
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
//        val setterForField =
//            if (field.hasAtLeastOneOfModifiers(Modifier.PRIVATE, Modifier.PROTECTED)) {
//                field.declaringClass.declaredMethods
//                    .filter { it.parameterTypes.size == 1 && it.parameterTypes[0] == field.type }
//                    .filter { it.name.startsWith("set") }
//                    .randomOrNull() ?: return null
//            } else null
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
            GreyBoxFuzzerGeneratorsAndSettings.generatorRepository.getOrProduceGenerator(
                parameterTypeContextForResolvedType,
                generatorContext,
                0
            )
                ?: return null
        var newFieldValue: UtModel = UtNullModel(parameterTypeContextForResolvedType.rawClass.id)
        for (i in 0 until 3) {
            try {
                generatorForField.generationState = GenerationState.REGENERATE
                generatorForField.generatorContext.startCheckpoint()
                newFieldValue = generatorForField.generateImpl(
                    GreyBoxFuzzerGeneratorsAndSettings.sourceOfRandomness,
                    GreyBoxFuzzerGeneratorsAndSettings.genStatus
                )
                if (newFieldValue !is UtNullModel) break
            } catch (e: Throwable) {
                continue
            }
        }
        logger.debug { "NEW FIELD VALUE = $newFieldValue" }
//        return if (setterForField == null) {
//            generatorForField to UtDirectSetFieldModel(clazzInstance, field.fieldId, newFieldValue)
//        } else {
//            generatorForField to UtExecutableCallModel(clazzInstance, setterForField.executableId, listOf(newFieldValue))
//        }
        return generatorForField to UtDirectSetFieldModel(clazzInstance, field.fieldId, newFieldValue)
    }

    private fun setNewFieldValue(
        field: Field,
        genericsContext: GenericsContext,
        clazzInstance: UtAssembleModel,
        generatorContext: GeneratorContext,
    ): UtStatementModel? =
        setNewFieldValueWithGenerator(field, genericsContext, clazzInstance, generatorContext)?.second

    fun mutateThisInstance(
        thisInstance: ThisInstance,
        fieldsToRegenerate: List<Field>,
        generatorContext: GeneratorContext
    ): ThisInstance {
        if (thisInstance !is NormalMethodThisInstance) return thisInstance
        val thisInstanceAsUtModel = thisInstance.utModel as? UtAssembleModel ?: return thisInstance
        val allClassFieldsFiltered = thisInstance.classId.allDeclaredFieldIds.toList()
            .mapNotNull {
                try {
                    it.jField
                } catch (e: Throwable) {
                    null
                }
            }
            .shuffled()
            .take(Random.nextInt(0, 4))
        val finalFieldsToRegenerate = (fieldsToRegenerate.filter { Random.getTrue(50) } + allClassFieldsFiltered).toSet()
        val mutationResult =
            regenerateFields(
                thisInstance.classId.jClass,
                thisInstanceAsUtModel,
                finalFieldsToRegenerate.toList(),
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
        val randomMethod = fParameter.classId.allMethods.toList().randomOrNull() ?: return fParameter
        val parametersForMethodInvocation =
            randomMethod.method.parameters.mapIndexed { index, parameter ->
                val resolvedParameterCtx =
                    originalParameter.resolveParameterTypeAndBuildParameterContext(index, randomMethod.method)
                val generatorForParameter =
                    GreyBoxFuzzerGeneratorsAndSettings.generatorRepository.getOrProduceGenerator(
                        resolvedParameterCtx,
                        generatorContext,
                        0
                    )
                        ?: return fParameter
                DataGenerator.generate(
                    generatorForParameter,
                    parameter,
                    GreyBoxFuzzerGeneratorsAndSettings.sourceOfRandomness,
                    GreyBoxFuzzerGeneratorsAndSettings.genStatus
                ).utModel
            }
        val callModel =
            UtExecutableCallModel(fParameter.utModel as UtReferenceModel, randomMethod, parametersForMethodInvocation)
        (originalUtModel as? UtAssembleModel)?.addModification(listOf(callModel))
        return FParameter(originalParameter, null, fParameter.utModel, fParameter.generator, fParameter.fields)
    }
}