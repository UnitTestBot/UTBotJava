package org.utbot.fuzzing.providers

import mu.KotlinLogging
import org.utbot.framework.plugin.api.DirectFieldAccessId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtDirectGetFieldModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.replaceWithWrapperIfPrimitive
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.toFuzzerType

class FieldValueProvider(
    private val idGenerator: IdGenerator<Int>,
    private val fieldId: FieldId,
) : JavaValueProvider {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun accept(type: FuzzedType): Boolean =
        replaceWithWrapperIfPrimitive(type.classId).jClass
            .isAssignableFrom(replaceWithWrapperIfPrimitive(fieldId.type).jClass)

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> = sequenceOf(
        Seed.Recursive(
            construct = Routine.Create(listOf(
                toFuzzerType(fieldId.declaringClass.jClass, description.typeCache)
            )) { values ->
                val thisInstanceValue = values.single()
                val thisInstanceModel = when (val model = thisInstanceValue.model) {
                    is UtReferenceModel -> model
                    is UtNullModel -> return@Create defaultFuzzedValue(type.classId)
                    else -> {
                        logger.warn { "This instance model can be only UtReferenceModel or UtNullModel, but $model is met" }
                        return@Create defaultFuzzedValue(type.classId)
                    }
                }
                UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = type.classId,
                    modelName = "${thisInstanceModel.modelName}.${fieldId.name}",
                    instantiationCall = UtDirectGetFieldModel(
                        instance = thisInstanceModel,
                        fieldAccess = DirectFieldAccessId(
                            classId = fieldId.declaringClass,
                            name = "<direct_get_${fieldId.name}>",
                            fieldId = fieldId
                        )
                    )
                ).fuzzed {
                    summary = "${thisInstanceValue.summary}.${fieldId.name}"
                }
            },
            modify = emptySequence(),
            empty = defaultValueRoutine(type.classId)
        )
    )
}