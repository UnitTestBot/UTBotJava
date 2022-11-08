package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.*

fun Type.isPythonType(): Boolean {
    return when (this) {
        is PythonCallable, is PythonCompositeType -> true
        is TypeParameter -> this.meta is PythonTypeVarMetaData
        is NamedType -> this.name in pythonNames
        else -> false
    }
}

val pythonAnyName = Name(listOf("typing"), "Any")
val pythonUnionName = Name(listOf("typing"), "Union")
val pythonNoneName = Name(emptyList(), "None")
val pythonTupleName = Name(listOf("typing"), "Tuple")
val pythonCallableName = Name(listOf("typing"), "Callable")
val overloadName = Name(emptyList(), "Overload")

val pythonNames: List<Name> = listOf(
    pythonAnyName,
    pythonUnionName,
    pythonNoneName,
    pythonTupleName,
    pythonCallableName,
    overloadName
)

val pythonAnyType = NamedTypeCreator.create(emptyList(), pythonAnyName)
val pythonNoneType = NamedTypeCreator.create(emptyList(), pythonNoneName)

fun createPythonUnionType(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), pythonUnionName, members)

fun createOverloadedFunctionType(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), overloadName, members)

fun createPythonTupleType(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), pythonTupleName, members)

interface PythonCallable: FunctionType {
    val name: Name
        get() = pythonCallableName
    val argumentKinds: List<ArgKind>
    enum class ArgKind {
        Positional
    }
}

interface PythonCompositeType: CompositeType {
    val memberNames: List<String>
    val namedMembers: List<PythonAttribute>
        get() = (memberNames zip members).map { PythonAttribute(it.first, it.second) }
    /*
    val mro: List<PythonCompositeType>
        get() {
            val result = mutableListOf(this)
            supertypes.forEach {

            }
        }
     */
}

interface PythonConcreteCompositeType: PythonCompositeType

interface PythonProtocol: PythonCompositeType {
    val protocolMemberNames: List<String>
}

class PythonAttribute(
    val name: String,
    val type: Type
) {
    override fun toString(): String =
        "$name: $type"
}

class PythonTypeVarMetaData(
    val name: String
): TypeParameterMetaData()

val exactTypeRelation = TypeRelation("=")
val upperBoundRelation = TypeRelation("<")