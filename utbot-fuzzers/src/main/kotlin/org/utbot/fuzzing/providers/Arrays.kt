package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.providers.ConstantsModelProvider.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider

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
                modify = Routine.ForEach(listOf(FuzzedType(type.classId.elementClassId!!))) { self, i, values ->
                    (self.model as UtArrayModel).stores[i] = values.first().model
                }
            ))
    }
}