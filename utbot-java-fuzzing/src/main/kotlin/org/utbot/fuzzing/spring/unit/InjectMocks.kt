package org.utbot.fuzzing.spring.unit

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.isFinal
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.jField
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.toFuzzerType

/**
 * Models created by this class can be used with `@InjectMock` annotation, because
 * they are [UtCompositeModel]s similar to the ones created by the symbolic engine.
 */
class InjectMockValueProvider(
    private val idGenerator: IdGenerator<Int>,
    private val classToUseCompositeModelFor: ClassId
) : JavaValueProvider {
    override fun accept(type: FuzzedType): Boolean = type.classId == classToUseCompositeModelFor

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> {
        val fields = type.classId.allDeclaredFieldIds.filterNot { it.isStatic && it.isFinal }.toList()
        return sequenceOf(Seed.Recursive(
            construct = Routine.Create(types = fields.map { toFuzzerType(it.jField.genericType, description.typeCache) }) { values ->
                emptyFuzzedValue(type.classId).also {
                    (it.model as UtCompositeModel).fields.putAll(
                        fields.zip(values).associate { (field, value) -> field to value.model }
                    )
                }
            },
            empty = Routine.Empty { emptyFuzzedValue(type.classId) }
        ))
    }

    private fun emptyFuzzedValue(classId: ClassId) = UtCompositeModel(
        id = idGenerator.createId(),
        classId = classId,
        isMock = false,
    ).fuzzed { summary = "%var% = ${classId.simpleName}()" }
}