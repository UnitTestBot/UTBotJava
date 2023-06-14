package org.utbot.fuzzing.spring

import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.providers.nullRoutine

class SavedEntityProvider(
    private val idGenerator: IdGenerator<Int>,
    private val repositoryId: SpringRepositoryId
) : JavaValueProvider {
    override fun accept(type: FuzzedType): Boolean = type.classId == repositoryId.entityClassId

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> =
        sequenceOf(
            Seed.Recursive(
                construct = Routine.Create(listOf(type)) { values ->
                    val entityValue = values.single()
                    val entityModel = entityValue.model
                    UtAssembleModel(
                        id = idGenerator.createId(),
                        classId = type.classId,
                        modelName = "${repositoryId.repositoryBeanName}.save(${(entityModel as? UtReferenceModel)?.modelName ?: "..."})",
                        instantiationCall = SpringModelUtils.createSaveCallModel(
                            repositoryId = repositoryId,
                            id = idGenerator.createId(),
                            entityModel = entityModel
                        )
                    ).fuzzed {
                        summary = "%var% = ${repositoryId.repositoryBeanName}.save(${entityValue.summary})"
                    }
                },
                modify = emptySequence(),
                empty = nullRoutine(type.classId)
            )
        )
}