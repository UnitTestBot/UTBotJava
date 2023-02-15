package org.utbot.python.newtyping.inference.constructors

import org.utbot.python.newtyping.general.Name
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.general.TypeParameter

class FakeClassStorage(
    initialFakeClasses: Set<Type> = emptySet(),
    initialTypeVars: Set<TypeParameter> = emptySet(),
    initialId: Int = 0
) {
    val fakeClasses: MutableSet<Type> = initialFakeClasses.toMutableSet()
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
}