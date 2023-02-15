package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.*

fun Type.isPythonType(): Boolean {
    return meta is PythonTypeDescription
}

fun Type.pythonDescription(): PythonTypeDescription {
    return meta as? PythonTypeDescription ?: error("Trying to get Python description from non-Python type $this")
}

fun Type.getPythonAttributes(): List<PythonDefinition> {
    return pythonDescription().getNamedMembers(this)
}

fun Type.getPythonAttributeByName(storage: PythonTypeStorage, name: String): PythonDefinition? {
    return pythonDescription().getMemberByName(storage, this, name)
}

fun Type.pythonAnnotationParameters(): List<Type> {
    return pythonDescription().getAnnotationParameters(this)
}

fun Type.pythonModules(): Set<String> {
    return pythonDescription().getModules(this)
}

fun Type.isPythonObjectType(): Boolean {
    if (!isPythonType())
        return false
    val description = pythonDescription()
    return description.name.prefix == listOf("builtins") && description.name.name == "object"
}

fun Type.pythonTypeRepresentation(): String {
    return pythonDescription().getTypeRepresentation(this)
}

fun Type.pythonTypeName(): String {
    return pythonDescription().getTypeName()
}

val pythonAnyName = Name(listOf("typing"), "Any")
val pythonUnionName = Name(listOf("typing"), "Union")
val pythonNoneName = Name(emptyList(), "None")
val pythonTupleName = Name(listOf("typing"), "Tuple")
val pythonCallableName = Name(listOf("typing"), "Callable")
val overloadName = Name(emptyList(), "Overload")

val pythonAnyType = createTypeWithMembers(PythonAnyTypeDescription, emptyList())
val pythonNoneType = createTypeWithMembers(PythonNoneTypeDescription, emptyList())

fun createPythonUnionType(members: List<Type>): Type =
    createTypeWithMembers(PythonUnionTypeDescription, members)

fun createOverload(members: List<Type>): Type =
    createTypeWithMembers(PythonOverloadTypeDescription, members)

fun createPythonTupleType(members: List<Type>): Type =
    createTypeWithMembers(PythonTupleTypeDescription, members)

fun createPythonConcreteCompositeType(
    name: Name,
    numberOfParameters: Int,
    memberDescriptions: List<PythonDefinitionDescription>,
    isAbstract: Boolean,
    isFakeClass: Boolean = false,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(
        numberOfParameters,
        PythonConcreteCompositeTypeDescription(name, memberDescriptions, isAbstract, isFakeClass),
        initialization
    )

fun createPythonProtocol(
    name: Name,
    numberOfParameters: Int,
    memberDescriptions: List<PythonDefinitionDescription>,
    protocolMemberNames: List<String>,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(
        numberOfParameters,
        PythonProtocolDescription(name, memberDescriptions, protocolMemberNames),
        initialization
    )

fun createPythonCallableType(
    numberOfParameters: Int,
    argumentKinds: List<PythonCallableTypeDescription.ArgKind>,
    argumentNames: List<String?>,
    initialization: (FunctionTypeCreator.Original) -> FunctionTypeCreator.InitializationData
): FunctionType =
    FunctionTypeCreator.create(
        numberOfParameters,
        PythonCallableTypeDescription(argumentKinds, argumentNames),
        initialization
    )

val exactTypeRelation = TypeRelation("=")
val upperBoundRelation = TypeRelation("<")