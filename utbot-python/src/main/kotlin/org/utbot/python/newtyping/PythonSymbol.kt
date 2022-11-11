package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.*

fun Type.isPythonType(): Boolean {
    return meta is PythonTypeDescription
}

fun Type.pythonDescription(): PythonTypeDescription {
    return meta as? PythonTypeDescription ?: error("Trying to get Python description from non-Python type $this")
}

fun Type.getPythonAttributes(): List<PythonAttribute> {
    return pythonDescription().getNamedMembers(this)
}

sealed class PythonTypeDescription: TypeMetaData() {
    open fun castToCompatibleTypeApi(type: Type): Type = type
    open fun getNamedMembers(type: Type): List<PythonAttribute> = emptyList()
    open fun getAnnotationParameters(type: Type): List<Type> = emptyList()
}

sealed class PythonCompositeTypeDescription(val memberNames: List<String>): PythonTypeDescription() {
    override fun castToCompatibleTypeApi(type: Type): CompositeType {
        return type as? CompositeType
            ?: error("Got unexpected type PythonCompositeTypeDescription: $type")
    }
    override fun getNamedMembers(type: Type): List<PythonAttribute> {
        val compositeType = castToCompatibleTypeApi(type)
        assert(compositeType.members.size == memberNames.size)
        return (memberNames zip compositeType.members).map { PythonAttribute(it.first, it.second) }
    }
    override fun getAnnotationParameters(type: Type): List<Type> = type.parameters
}

sealed class PythonSpecialAnnotation: PythonTypeDescription()

class PythonTypeVarDescription(val name: String): PythonTypeDescription()

// Composite types
class PythonConcreteCompositeTypeDescription(memberNames: List<String>): PythonCompositeTypeDescription(memberNames)
class PythonProtocolDescription(
    memberNames: List<String>,
    val protocolMemberNames: List<String>
): PythonCompositeTypeDescription(memberNames)

// Special annotations
object PythonAnyTypeDescription: PythonSpecialAnnotation()

object PythonNoneTypeDescription: PythonSpecialAnnotation() {
    override fun getNamedMembers(type: Type): List<PythonAttribute> =
        emptyList() // TODO: add None attributes
}

object PythonUnionTypeDescription: PythonSpecialAnnotation() {
    override fun castToCompatibleTypeApi(type: Type): StatefulType {
        return type as? StatefulType
            ?: error("Got unexpected type PythonUnionTypeDescription: $type")
    }
    override fun getNamedMembers(type: Type): List<PythonAttribute> {
        val statefulType = castToCompatibleTypeApi(type)
        TODO("Not yet implemented")
    }
    override fun getAnnotationParameters(type: Type): List<Type> {
        val statefulType = castToCompatibleTypeApi(type)
        return statefulType.members
    }
}

object PythonOverloadTypeDescription: PythonSpecialAnnotation() {
    // TODO: add logic
}

object PythonTupleTypeDescription: PythonSpecialAnnotation() {
    // TODO: add logic
}

class PythonCallableTypeDescription(val argumentKinds: List<ArgKind>): PythonSpecialAnnotation() {
    val name = pythonCallableName
    override fun castToCompatibleTypeApi(type: Type): FunctionType {
        return type as? FunctionType
            ?: error("Got unexpected type PythonCallableTypeDescription: $type")
    }
    override fun getNamedMembers(type: Type): List<PythonAttribute> {
        val functionType = castToCompatibleTypeApi(type)
        return listOf(PythonAttribute("__call__", functionType))
    }
    override fun getAnnotationParameters(type: Type): List<Type> {
        val functionType = castToCompatibleTypeApi(type)
        return functionType.arguments + listOf(functionType.returnValue)
    }
    enum class ArgKind {
        Positional
    }
}

val pythonAnyName = Name(listOf("typing"), "Any")
val pythonUnionName = Name(listOf("typing"), "Union")
val pythonNoneName = Name(emptyList(), "None")
val pythonTupleName = Name(listOf("typing"), "Tuple")
val pythonCallableName = Name(listOf("typing"), "Callable")
val overloadName = Name(emptyList(), "Overload")

val pythonAnyType = NamedTypeCreator.create(emptyList(), pythonAnyName, PythonAnyTypeDescription)
val pythonNoneType = NamedTypeCreator.create(emptyList(), pythonNoneName, PythonNoneTypeDescription)

fun createPythonUnionType(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), pythonUnionName, members, PythonUnionTypeDescription)

fun createOverloadedFunctionType(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), overloadName, members, PythonOverloadTypeDescription)

fun createPythonTupleType(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), pythonTupleName, members, PythonTupleTypeDescription)

fun createPythonConcreteCompositeType(
    name: Name,
    numberOfParameters: Int,
    memberNames: List<String>,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(name, numberOfParameters, PythonConcreteCompositeTypeDescription(memberNames), initialization)

fun createPythonProtocol(
    name: Name,
    numberOfParameters: Int,
    memberNames: List<String>,
    protocolMemberNames: List<String>,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(name, numberOfParameters, PythonProtocolDescription(memberNames, protocolMemberNames), initialization)

fun createPythonCallableType(
    numberOfParameters: Int,
    argumentKinds: List<PythonCallableTypeDescription.ArgKind>,
    initialization: (FunctionTypeCreator.Original) -> FunctionTypeCreator.InitializationData
): FunctionType =
    FunctionTypeCreator.create(numberOfParameters, PythonCallableTypeDescription(argumentKinds), initialization)

class PythonAttribute(
    val name: String,
    val type: Type
) {
    override fun toString(): String =
        "$name: $type"
}

val exactTypeRelation = TypeRelation("=")
val upperBoundRelation = TypeRelation("<")

/*
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

 */

/*
interface PythonConcreteCompositeType: PythonCompositeType


interface PythonProtocol: PythonCompositeType {
    val protocolMemberNames: List<String>
}

class PythonTypeVarMetaData(
    val name: String
): TypeParameterMetaData()

 */