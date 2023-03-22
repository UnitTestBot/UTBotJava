package org.utbot.python.newtyping.inference.constructors

import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.Name
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.general.TypeParameter

class FakeClassStorage(
    initialFakeClasses: Set<Type> = emptySet(),
    initialTypeVars: Set<TypeParameter> = emptySet(),
    initialId: Int = 0
) {
    private val fakeClasses: MutableSet<Type> = initialFakeClasses.toMutableSet()
    val typeVars: MutableSet<TypeParameter> = initialTypeVars.toMutableSet()
    private var id = initialId

    fun getName(): Name {
        return Name(emptyList(), "_${id++}")
    }

    fun addFakeClass(fakeClass: Type) {
        fakeClasses.add(fakeClass)
        typeVars.addAll(fakeClass.parameters.mapNotNull { it as? TypeParameter })
    }

    fun copy(): FakeClassStorage =
        FakeClassStorage(fakeClasses, typeVars, id)

    fun getAdditionalVars(): String {
        val typeVarsStr = typeVars.joinToString("\n") { typeVar ->
            val meta = typeVar.pythonDescription()
            "${meta.name.name} = typing.TypeVar(\"${meta.name.name}\")"
        }
        val classesStr = fakeClasses.joinToString("\n\n") { type ->
            val meta = type.pythonDescription()
            """
                class ${meta.name.name}:
                    ${type.getPythonAttributes().joinToString("\n") {attr -> 
                        "${attr.meta.name}: ${attr.type.pythonTypeRepresentation()}"
                    }}
            """.trimIndent()
        }
        return typeVarsStr + "\n" + classesStr
    }
}