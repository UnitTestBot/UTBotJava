package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonTupleClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.activateAny
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utbot.python.newtyping.PythonSubtypeChecker
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonAnyType
import org.utbot.python.newtyping.typesAreEqual

object TupleValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonTupleClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        yieldAll(getConstants(description, type))
        val param = type.utType.pythonAnnotationParameters()
        yield(
            Seed.Collection(
                construct = Routine.Collection {
                    PythonFuzzedValue(
                        PythonTree.TupleNode(
                            emptyMap<Int, PythonTree.PythonTreeNode>().toMutableMap(),
                        ),
                        "%var% = ${type.pythonTypeRepresentation()}"
                    )
                },
                modify = Routine.ForEach(param.toFuzzed().activateAny()) { self, i, values ->
                    (self.tree as PythonTree.TupleNode).items[i] = values.first().tree
                }
            ))
    }

    private fun getConstants(description: PythonMethodDescription, type: FuzzedUtType): List<Seed<FuzzedUtType, PythonFuzzedValue>> {
        if (!typesAreEqual(type.utType.parameters.first(), pythonAnyType))
            return getSuitableConstantsFromCode(description, type)
        return emptyList()
    }
    private fun getSuitableConstantsFromCode(description: PythonMethodDescription, type: FuzzedUtType): List<Seed<FuzzedUtType, PythonFuzzedValue>> {
        return description.concreteValues.filter {
            PythonSubtypeChecker.checkIfRightIsSubtypeOfLeft(type.utType, it.type, description.pythonTypeStorage)
        }.mapNotNull { value ->
            PythonTree.fromParsedConstant(Pair(value.type, value.value))?.let {
                Seed.Simple(PythonFuzzedValue(it))
            }
        }
    }

}