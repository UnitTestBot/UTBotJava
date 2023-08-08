package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.*

fun UtType.isPythonType(): Boolean {
    return meta is PythonTypeDescription
}

fun UtType.pythonDescription(): PythonTypeDescription {
    return meta as? PythonTypeDescription ?: error("Trying to get Python description from non-Python type $this")
}

fun UtType.getPythonAttributes(): List<PythonDefinition> {
    return pythonDescription().getNamedMembers(this)
}

fun UtType.getPythonAttributeByName(storage: PythonTypeStorage, name: String): PythonDefinition? {
    return pythonDescription().getMemberByName(storage, this, name)
}

fun UtType.pythonAnnotationParameters(): List<UtType> {
    return pythonDescription().getAnnotationParameters(this)
}

fun UtType.pythonModules(): Set<String> {
    return pythonDescription().getModules(this)
}

fun UtType.isPythonObjectType(): Boolean {
    if (!isPythonType())
        return false
    val description = pythonDescription()
    return description.name.prefix == listOf("builtins") && description.name.name == "object"
}

fun UtType.pythonTypeRepresentation(): String {
    return pythonDescription().getTypeRepresentation(this)
}

fun UtType.pythonTypeName(): String {
    return pythonDescription().getTypeName()
}

fun UtType.pythonModuleName(): String {
    return pythonDescription().getModuleName()
}

fun UtType.pythonName(): String {
    return pythonDescription().getName()
}

val pythonAnyName = Name(listOf("typing"), "Any")
val pythonUnionName = Name(listOf("typing"), "Union")
val pythonNoneName = Name(emptyList(), "None")
val pythonTupleName = Name(listOf("typing"), "Tuple")
val pythonCallableName = Name(listOf("typing"), "Callable")
val overloadName = Name(emptyList(), "Overload")
val pythonTypeAliasName = Name(listOf("typing"), "TypeAlias")

val pythonAnyType = createTypeWithMembers(PythonAnyTypeDescription, emptyList())
val pythonNoneType = createTypeWithMembers(PythonNoneTypeDescription, emptyList())

fun createPythonUnionType(members: List<UtType>): UtType =
    createTypeWithMembers(PythonUnionTypeDescription, members)

fun createOverload(members: List<UtType>): UtType =
    createTypeWithMembers(PythonOverloadTypeDescription, members)

fun createPythonTupleType(members: List<UtType>): UtType =
    createTypeWithMembers(PythonTupleTypeDescription, members)

fun createPythonTypeAlias(initialization: (UtType) -> UtType): CompositeType =
    CompositeTypeCreator.create(0, PythonTypeAliasDescription) { self ->
        CompositeTypeCreator.InitializationData(
            members = listOf(initialization(self)),
            supertypes = emptyList()
        )
    }

fun createPythonConcreteCompositeType(
    name: Name,
    numberOfParameters: Int,
    memberDescriptions: List<PythonDefinitionDescription>,
    isAbstract: Boolean,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(
        numberOfParameters,
        PythonConcreteCompositeTypeDescription(name, memberDescriptions, isAbstract),
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