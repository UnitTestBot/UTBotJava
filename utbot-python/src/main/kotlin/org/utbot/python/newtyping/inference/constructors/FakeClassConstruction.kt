package org.utbot.python.newtyping.inference.constructors


import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.*

fun fakeClassCanBeConstructed(type: Type, storage: PythonTypeStorage): Boolean {
    if (type !is CompositeType || type.pythonDescription() !is PythonConcreteCompositeTypeDescription)
        return false

    val filteredSupertypes = type.supertypes.filterNot { typesAreEqual(it, storage.pythonObject) }

    if (filteredSupertypes.isNotEmpty() || type.parameters.isNotEmpty())
        return false

    val constructor = type.getPythonAttributeByName(storage, "__init__")?.type
    val constructors = when (constructor?.pythonDescription()) {
        is PythonCallableTypeDescription -> listOf(constructor)
        is PythonOverloadTypeDescription -> constructor.pythonAnnotationParameters()
        else -> return false
    }

    return constructors.all { cur ->
        (cur as? FunctionType)?.arguments?.any { typesAreEqual(it, pythonAnyType) } ?: true
    }
}

fun constructFakeClass(
    originalClass: CompositeType,
    description: PythonConcreteCompositeTypeDescription,
    fakeClassStorage: FakeClassStorage
): CompositeType {
    val result = createPythonConcreteCompositeType(
        fakeClassStorage.getName(),
        0,
        description.memberDescriptions,
        description.isAbstract,
        isFakeClass = true
    ) { self ->
        val members = description.getNamedMembers(originalClass).map {
            when (val meta = it.meta) {
                is PythonFunctionDescription -> {
                    it.type
                }
                is PythonVariableDescription -> {
                    if (typesAreEqual(it.type, pythonAnyType) && !meta.isInitializedInClass) {
                        self.parameters.add(TypeParameter(self))
                        self.parameters.last().meta = PythonTypeVarDescription(
                            fakeClassStorage.getName(),
                            PythonTypeVarDescription.Variance.INVARIANT,
                            PythonTypeVarDescription.ParameterKind.WithUpperBound
                        )
                        self.parameters.last()
                    } else {
                        it.type
                    }
                }
            }
        }
        CompositeTypeCreator.InitializationData(
            members,
            emptyList()
        )
    }
    fakeClassStorage.addFakeClass(result)
    return result
}