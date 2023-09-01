package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isConcreteType
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.PythonSubtypeChecker.Companion.checkIfRightIsSubtypeOfLeft
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.UtType

class SubtypeValueProvider(
    private val typeStorage: PythonTypeHintsStorage
) : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.meta is PythonProtocolDescription ||
                ((type.meta as? PythonConcreteCompositeTypeDescription)?.isAbstract == true)
    }

    private val concreteTypes = typeStorage.simpleTypes.filter {
        isConcreteType(it) && it.pythonDescription().name.name.first() != '_'  // Don't substitute private classes
    }.map {
        DefaultSubstitutionProvider.substituteAll(it, it.parameters.map { pythonAnyType })
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        val subtypes = concreteTypes.filter { checkIfRightIsSubtypeOfLeft(type, it, typeStorage) }
        subtypes.forEach { subtype ->
            yield(
                Seed.Recursive(
                construct = Routine.Create(listOf(subtype)) { v -> v.first() },
                empty = Routine.Empty { PythonFuzzedValue(PythonTree.FakeNode) }
            ))
        }
    }
}