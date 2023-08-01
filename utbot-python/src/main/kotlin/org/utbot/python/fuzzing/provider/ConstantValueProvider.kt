package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.fuzzing.value.TypesFromJSONStorage

object ConstantValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return TypesFromJSONStorage.getTypesFromJsonStorage().containsKey(type.pythonTypeName()) || type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonFuzzedValue>> =
        sequence {
            val storage = TypesFromJSONStorage.getTypesFromJsonStorage()
            storage.values.forEach { values ->
                val constants = if (values.name == type.pythonTypeName()) {
                    values.instances
                } else {
                    emptyList()
                }
                constants.forEach {
                    yield(
                        Seed.Simple(
                            PythonFuzzedValue(
                                PythonTree.PrimitiveNode(
                                    PythonClassId(values.name),
                                    it
                                ),
                                "%var% = $it"
                            )
                        )
                    )
                }
            }
        }
}