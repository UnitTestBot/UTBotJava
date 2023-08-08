package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.Bool
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.framework.api.python.util.pythonRePatternClassId
import org.utbot.python.framework.api.python.util.toPythonRepr
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.fuzzing.provider.utils.makeRawString
import org.utbot.python.fuzzing.provider.utils.transformRawString
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName

object RePatternValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription>{
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == pythonRePatternClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        yield(Seed.Recursive(
            construct = Routine.Create(
                listOf(
                    description.pythonTypeStorage.pythonStr,
                )
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