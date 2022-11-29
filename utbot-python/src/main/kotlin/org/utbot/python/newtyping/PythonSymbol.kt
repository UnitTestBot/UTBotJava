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

fun Type.getPythonAttributeByName(name: String): PythonAttribute? {
    return pythonDescription().getMemberByName(this, name)
}

object BuiltinTypes {
    lateinit var pythonObject: Type
    lateinit var pythonBool: Type
    lateinit var pythonList: Type
    lateinit var pythonDict: Type
    lateinit var pythonSet: Type
    lateinit var pythonInt: Type
    lateinit var pythonFloat: Type
    lateinit var pythonComplex: Type
    lateinit var pythonStr: Type

    fun checkInitialized() {
        val inits = listOf(
            this::pythonObject.isInitialized,
            this::pythonBool.isInitialized,
            this::pythonList.isInitialized,
            this::pythonDict.isInitialized,
            this::pythonSet.isInitialized,
            this::pythonInt.isInitialized,
            this::pythonFloat.isInitialized,
            this::pythonComplex.isInitialized,
            this::pythonStr.isInitialized
        )
        if (!inits.all { it })
            error("Some types from BuiltinTypes were skipped during initialization")
    }
}

sealed class PythonTypeDescription(name: Name): TypeMetaDataWithName(name) {
    open fun castToCompatibleTypeApi(type: Type): Type = type
    open fun getNamedMembers(type: Type): List<PythonAttribute> = emptyList()  // direct members (without inheritance)
    open fun getAnnotationParameters(type: Type): List<Type> = emptyList()
    open fun getMemberByName(type: Type, name: String): PythonAttribute? =  // overridden for some types
        getNamedMembers(type).find { it.name == name }
}

sealed class PythonCompositeTypeDescription(
    name: Name,
    private val memberNames: List<String>
): PythonTypeDescription(name) {
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
    fun mro(type: Type): List<Type> {
        val compositeType = castToCompatibleTypeApi(type)
        var bases = compositeType.supertypes
        if (bases.isEmpty() && type.pythonDescription().name != BuiltinTypes.pythonObject.pythonDescription().name)
            bases = listOf(BuiltinTypes.pythonObject)
        val linBases = (bases.map {
            val description = it.meta as? PythonCompositeTypeDescription
                ?: error("Not a PythonCompositeType in superclasses of PythonCompositeType")
            description.mro(it)
        } + listOf(bases)).map { it.toMutableList() }.toMutableList()
        val result = mutableListOf(type)
        while (true) {
            linBases.removeIf { it.isEmpty() }
            if (linBases.isEmpty())
                break
            lateinit var addAtThisIteration: Type
            for (seq in linBases) {
                val head = seq.first()
                val isContainedSomewhereElse = linBases.any {
                    it.drop(1).any { type -> type.pythonDescription().name == head.pythonDescription().name }
                }
                if (!isContainedSomewhereElse) {
                    addAtThisIteration = head
                    break
                }
            }
            linBases.forEach {
                if (it.first().pythonDescription().name == addAtThisIteration.pythonDescription().name)
                    it.removeFirst()
            }
            result.add(addAtThisIteration)
        }
        return result
    }
    override fun getMemberByName(type: Type, name: String): PythonAttribute? {
        for (parent in mro(type)) {
            val cur = parent.getPythonAttributes().find { it.name == name }
            if (cur != null)
                return cur
        }
        return null
    }
}

sealed class PythonSpecialAnnotation(name: Name): PythonTypeDescription(name)

class PythonTypeVarDescription(
    name: Name,
    val variance: Variance,
    val parameterKind: ParameterKind
): PythonTypeDescription(name) {
    override fun castToCompatibleTypeApi(type: Type): TypeParameter {
        return type as? TypeParameter
            ?: error("Got unexpected type PythonTypeVarDescription: $type")
    }
    enum class Variance {
        INVARIANT,
        COVARIANT,
        CONTRAVARIANT
    }
    enum class ParameterKind {
        WithUpperBound,
        WithConcreteValues
    }
}

// Composite types
class PythonConcreteCompositeTypeDescription(
    name: Name,
    memberNames: List<String>
): PythonCompositeTypeDescription(name, memberNames)
class PythonProtocolDescription(
    name: Name,
    memberNames: List<String>,
    val protocolMemberNames: List<String>
): PythonCompositeTypeDescription(name, memberNames)

class PythonCallableTypeDescription(
    val argumentKinds: List<ArgKind>,
    val argumentNames: List<String>,
    val isClassMethod: Boolean,
    val isStaticMethod: Boolean
): PythonTypeDescription(pythonCallableName) {
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

// Special Python annotations
object PythonAnyTypeDescription: PythonSpecialAnnotation(pythonAnyName) {
    override fun getMemberByName(type: Type, name: String): PythonAttribute {
        return PythonAttribute(name, pythonAnyType)
    }
}

object PythonNoneTypeDescription: PythonSpecialAnnotation(pythonNoneName) {
    override fun getNamedMembers(type: Type): List<PythonAttribute> =
        TODO("Not yet implemented")
}

object PythonUnionTypeDescription: PythonSpecialAnnotation(pythonUnionName) {
    override fun castToCompatibleTypeApi(type: Type): StatefulType {
        return type as? StatefulType
            ?: error("Got unexpected type PythonUnionTypeDescription: $type")
    }
    override fun getMemberByName(type: Type, name: String): PythonAttribute? {
        val statefulType = castToCompatibleTypeApi(type)
        val children = statefulType.members.mapNotNull {
            it.getPythonAttributeByName(name)?.type
        }
        return if (children.isEmpty())
            null
        else
            PythonAttribute(
                name = name,
                type = createPythonUnionType(children)
            )
    }
    override fun getAnnotationParameters(type: Type): List<Type> = castToCompatibleTypeApi(type).members
}

object PythonOverloadTypeDescription: PythonSpecialAnnotation(overloadName) {
    override fun castToCompatibleTypeApi(type: Type): StatefulType {
        return type as? StatefulType
            ?: error("Got unexpected type PythonOverloadTypeDescription: $type")
    }
    override fun getAnnotationParameters(type: Type): List<Type> = castToCompatibleTypeApi(type).members
    override fun getNamedMembers(type: Type): List<PythonAttribute> {
        val statefulType = castToCompatibleTypeApi(type)
        TODO("Not yet implemented")
    }
}

object PythonTupleTypeDescription: PythonSpecialAnnotation(pythonTupleName) {
    override fun castToCompatibleTypeApi(type: Type): StatefulType {
        return type as? StatefulType
            ?: error("Got unexpected type PythonTupleTypeDescription: $type")
    }
    override fun getAnnotationParameters(type: Type): List<Type> = castToCompatibleTypeApi(type).members
    override fun getNamedMembers(type: Type): List<PythonAttribute> {
        val statefulType = castToCompatibleTypeApi(type)
        TODO("Not yet implemented")
    }
}

val pythonAnyName = Name(listOf("typing"), "Any")
val pythonUnionName = Name(listOf("typing"), "Union")
val pythonNoneName = Name(emptyList(), "None")
val pythonTupleName = Name(listOf("typing"), "Tuple")
val pythonCallableName = Name(listOf("typing"), "Callable")
val overloadName = Name(emptyList(), "Overload")

val pythonAnyType = TypeCreator.create(emptyList(), PythonAnyTypeDescription)
val pythonNoneType = TypeCreator.create(emptyList(), PythonNoneTypeDescription)

fun createPythonUnionType(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), members, PythonUnionTypeDescription)

fun createOverload(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), members, PythonOverloadTypeDescription)

fun createPythonTupleType(members: List<Type>): StatefulType =
    StatefulTypeCreator.create(emptyList(), members, PythonTupleTypeDescription)

fun createPythonConcreteCompositeType(
    name: Name,
    numberOfParameters: Int,
    memberNames: List<String>,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(numberOfParameters, PythonConcreteCompositeTypeDescription(name, memberNames), initialization)

fun createPythonProtocol(
    name: Name,
    numberOfParameters: Int,
    memberNames: List<String>,
    protocolMemberNames: List<String>,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(numberOfParameters, PythonProtocolDescription(name, memberNames, protocolMemberNames), initialization)

fun createPythonCallableType(
    numberOfParameters: Int,
    argumentKinds: List<PythonCallableTypeDescription.ArgKind>,
    argumentNames: List<String>,
    isClassMethod: Boolean,
    isStaticMethod: Boolean,
    initialization: (FunctionTypeCreator.Original) -> FunctionTypeCreator.InitializationData
): FunctionType =
    FunctionTypeCreator.create(
        numberOfParameters,
        PythonCallableTypeDescription(argumentKinds, argumentNames, isClassMethod, isStaticMethod),
        initialization
    )

class PythonAttribute(
    val name: String,
    val type: Type
) {
    override fun toString(): String =
        "$name: $type"
}

val exactTypeRelation = TypeRelation("=")
val upperBoundRelation = TypeRelation("<")