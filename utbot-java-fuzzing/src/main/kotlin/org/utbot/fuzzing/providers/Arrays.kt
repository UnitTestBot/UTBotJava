package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.*

class ArrayValueProvider(
    val idGenerator: IdGenerator<Int>,
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {

    override fun accept(type: FuzzedType) = type.classId.isArray

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence<Seed<FuzzedType, FuzzedValue>> {
        yield(
            Seed.Collection(
                construct = Routine.Collection {
                    UtArrayModel(
                        id = idGenerator.createId(),
                        classId = type.classId,
                        length = it,
                        constModel = type.classId.elementClassId!!.defaultValueModel(),
                        stores = hashMapOf(),
                    ).fuzzed {
                        summary = "%var% = ${type.classId.elementClassId!!.simpleName}[$it]"
                    }
                },
                modify = Routine.ForEach(listOf(resolveArrayElementType(type))) { self, i, values ->
                    (self.model as UtArrayModel).stores[i] = values.first().model
                }
            ))
    }

    /**
     * Resolves array's element type. In case a generic type is used, like `T[]` the type should pass generics.
     *
     * For example, List<Number>[] returns List<Number>.
     */
    private fun resolveArrayElementType(arrayType: FuzzedType): FuzzedType = when {
        !arrayType.classId.isArray -> error("$arrayType is not array")
        arrayType.generics.size == 1 -> arrayType.generics.first()
        arrayType.generics.isEmpty() -> FuzzedType(
            arrayType.classId.elementClassId ?: error("Element classId of $arrayType is not found")
        )

        else -> error("Array has ${arrayType.generics.size} generic type for ($arrayType), that should not happen")
    }
}