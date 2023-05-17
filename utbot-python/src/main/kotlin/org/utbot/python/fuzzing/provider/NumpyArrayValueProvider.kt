package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

object NumpyArrayValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == "numpy.ndarray"
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        print(type)
        val param = listOf(type.parameters.last().parameters.first())
        yield(Seed.Collection(
            construct = Routine.Collection {
                PythonFuzzedValue(
                    PythonTree.ReduceNode(
                        PythonClassId("numpy.array"),
                        PythonClassId("numpy.array"),
                        mutableListOf(),
                    ),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = Routine.ForEach(param) { self, i, values ->
                (self.tree as PythonTree.ReduceNode).args[i] = values.first().tree
            }
        ))

//            construct = Routine.Create(
//                listOf(
//                    type.parameters.last()
//                )
//            ) { v ->
//                val value = v.first().tree as PythonTree.ListNode
//                PythonFuzzedValue(
//                    PythonTree.ReduceNode(
//                        PythonClassId("numpy.array"),
//                        PythonClassId("numpy.array"),
//                        listOf(value)
//                    ),
//                    "%var% = ${type.pythonTypeRepresentation()}",
//                )
//            },
//            empty = Routine.Empty {
//                PythonFuzzedValue(
//                    PythonTree.ReduceNode(
//                        PythonClassId("numpy.array"),
//                        PythonClassId("numpy.array"),
//                        listOf(PythonTree.ListNode(mutableMapOf()))
//                    ),
//                    "%var% = ${type.pythonTypeRepresentation()}"
//                )
//            }
//        ))
    }
}