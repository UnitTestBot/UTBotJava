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
import org.utbot.fuzzing.Scope
import org.utbot.fuzzing.ScopeProperty
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.toFuzzerType

val INJECT_MOCK_FLAG = ScopeProperty<Unit>(
    "INJECT_MOCK_FLAG is present if composite model should be used (i.e. thisInstance is being created)"
)

/**
 * Models created by this class can be used with `@InjectMock` annotation, because
 * they are [UtCompositeModel]s similar to the ones created by the symbolic engine.
 *
 * This class only creates models for thisInstance of type [classUnderTest].
 */
class InjectMockValueProvider(
    private val idGenerator: IdGenerator<Int>,
    private val classUnderTest: ClassId
) : JavaValueProvider {
    override fun enrich(description: FuzzedDescription, type: FuzzedType, scope: Scope) {
        // any value except this
        if (description.description.isStatic == false && scope.parameterIndex == 0 && scope.recursionDepth == 1) {
            scope.putProperty(INJECT_MOCK_FLAG, Unit)
        }
    }

    override fun accept(type: FuzzedType): Boolean = type.classId == classUnderTest

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> {
        if (description.scope?.getProperty(INJECT_MOCK_FLAG) == null) return emptySequence()
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