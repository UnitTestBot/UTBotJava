package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonRePatternClassId
import org.utbot.python.framework.api.python.util.toPythonRepr
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utbot.python.fuzzing.provider.utils.makeRawString

object RePatternValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonRePatternClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        yield(Seed.Recursive(
            construct = Routine.Create(
                listOf(
                    description.pythonTypeStorage.pythonStr,
                ).toFuzzed()
            ) { v ->
                val value = v.first().tree as PythonTree.PrimitiveNode
                val rawValue = value.repr.toPythonRepr().makeRawString()
                PythonFuzzedValue(
                    PythonTree.ReduceNode(
                        pythonRePatternClassId,
                        PythonClassId("re.compile"),
                        listOf(PythonTree.fromString(rawValue))
                    ),
                    "%var% = re.compile(${rawValue})"
                )
            },
            empty = Routine.Empty {
                PythonFuzzedValue(
                    PythonTree.PrimitiveNode(
                        pythonRePatternClassId,
                        "re.compile('')"
                    ),
                    "%var% = re.compile('')"
                )
            }
        ))
    }
}