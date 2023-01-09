package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type
import org.utbot.python.typing.PythonTypesStorage

object ConstantValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        if (meta is PythonConcreteCompositeTypeDescription) {
            return PythonTypesStorage.getTypesFromJsonStorage().containsKey(meta.name.toString())
        }
        return type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonFuzzedValue>> = sequence {
        val storage = PythonTypesStorage.getTypesFromJsonStorage()
        val meta = type.meta

        storage.values.forEach { values ->
            val constants = if (meta is PythonConcreteCompositeTypeDescription) {
                if (values.name == meta.name.toString()) {
                    values.instances
                } else {
                    emptyList()
                }
            } else {
                values.instances
            }
            constants.forEach {
                yield(Seed.Simple(
                    PythonFuzzedValue(
                        PythonTree.PrimitiveNode(
                            PythonClassId(values.name),
                            it
                        ),
                        "%var% = $it"
                    )
                ))
            }
        }
    }
}