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

fun Type.getPythonAttributeByName(storage: PythonTypeStorage, name: String): PythonAttribute? {
    return pythonDescription().getMemberByName(storage, this, name)
}

fun Type.pythonAnnotationParameters(): List<Type> {
    return pythonDescription().getAnnotationParameters(this)
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

class PythonTypeStorage(
    val pythonObject: Type,
    val pythonBool: Type,
    val pythonList: Type,
    val pythonDict: Type,
    val pythonSet: Type,
    val pythonInt: Type,
    val pythonFloat: Type,
    val pythonComplex: Type,
    val pythonStr: Type,
    val allTypes: Set<Type>
) {
    companion object {
        fun get(mypyStorage: MypyAnnotationStorage): PythonTypeStorage {
            val module = mypyStorage.definitions["builtins"]!!
            val allTypes: MutableSet<Type> = mutableSetOf()
            mypyStorage.definitions.forEach { (_, curModule) ->
                curModule.values.forEach {
                    if (it.kind == DefinitionType.Type)
                        allTypes.add(it.annotation.asUtBotType)
                }
            }
            return PythonTypeStorage(
                pythonObject = module["object"]!!.annotation.asUtBotType,
                pythonBool = module["bool"]!!.annotation.asUtBotType,
                pythonList = module["list"]!!.annotation.asUtBotType,
                pythonDict = module["dict"]!!.annotation.asUtBotType,
                pythonSet = module["set"]!!.annotation.asUtBotType,
                pythonInt = module["int"]!!.annotation.asUtBotType,
                pythonFloat = module["float"]!!.annotation.asUtBotType,
                pythonComplex = module["complex"]!!.annotation.asUtBotType,
                pythonStr = module["str"]!!.annotation.asUtBotType,
                allTypes = allTypes
            )
        }
    }
}

sealed class PythonTypeDescription(name: Name) : TypeMetaDataWithName(name) {
    open fun castToCompatibleTypeApi(type: Type): Type = type
    open fun getNamedMembers(type: Type): List<PythonAttribute> = emptyList()  // direct members (without inheritance)
    open fun getAnnotationParameters(type: Type): List<Type> = emptyList()
    open fun getMemberByName(storage: PythonTypeStorage, type: Type, name: String): PythonAttribute? =
        // overridden for some types
        getNamedMembers(type).find { it.name == name }
    open fun createTypeWithNewAnnotationParameters(like: Type, newParams: List<Type>): Type =  // overriden for Callable
        DefaultSubstitutionProvider.substituteAll(like.getOrigin(), newParams)
    open fun getTypeRepresentation(type: Type): String {  // overriden for Callable
        val root =
            if (name.prefix.isEmpty())
                name.name
            else
                name.prefix.joinToString() + "." + name.name
        val params = getAnnotationParameters(type)
        if (params.isEmpty())
            return root
        return "$root[${params.joinToString { it.pythonTypeRepresentation() }}]"
    }
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
    fun mro(storage: PythonTypeStorage, type: Type): List<Type> {
        val compositeType = castToCompatibleTypeApi(type)
        var bases = compositeType.supertypes
        if (bases.isEmpty() && !type.isPythonObjectType())
            bases = listOf(storage.pythonObject)
        val linBases = (bases.map {
            val description = it.meta as? PythonCompositeTypeDescription
                ?: error("Not a PythonCompositeType in superclasses of PythonCompositeType")
            description.mro(storage, it)
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

    override fun getMemberByName(storage: PythonTypeStorage, type: Type, name: String): PythonAttribute? {
        for (parent in mro(storage, type)) {
            val cur = parent.getPythonAttributes().find { it.name == name }
            if (cur != null)
                return cur
        }
        return null
    }
}

sealed class PythonSpecialAnnotation(name: Name) : PythonTypeDescription(name)

class PythonTypeVarDescription(
    name: Name,
    val variance: Variance,
    val parameterKind: ParameterKind
) : PythonTypeDescription(name) {
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
    memberNames: List<String>,
    val isAbstract: Boolean
) : PythonCompositeTypeDescription(name, memberNames)

class PythonProtocolDescription(
    name: Name,
    memberNames: List<String>,
    val protocolMemberNames: List<String>
) : PythonCompositeTypeDescription(name, memberNames)

class PythonCallableTypeDescription(
    val argumentKinds: List<ArgKind>,
    val argumentNames: List<String>,
    val isClassMethod: Boolean,
    val isStaticMethod: Boolean
): PythonTypeDescription(pythonCallableName) {
    val numberOfArguments = argumentNames.size
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

    override fun createTypeWithNewAnnotationParameters(like: Type, newParams: List<Type>): Type {
        val args = newParams.dropLast(1)
        val returnValue = newParams.last()
        return createPythonCallableType(
            like.parameters.size,
            argumentKinds,
            argumentNames,
            isClassMethod,
            isStaticMethod
        ) { self ->
            val oldToNewParameters = (like.parameters zip self.parameters).associate {
                (it.first as TypeParameter) to it.second
            }
            val newArgs = args.map {
                DefaultSubstitutionProvider.substitute(it, oldToNewParameters)
            }
            val newReturnValue = DefaultSubstitutionProvider.substitute(returnValue, oldToNewParameters)
            FunctionTypeCreator.InitializationData(
                arguments = newArgs,
                returnValue = newReturnValue
            )
        }
    }

    override fun getTypeRepresentation(type: Type): String {
        val functionType = castToCompatibleTypeApi(type)
        val root = name.prefix.joinToString(".") + "." + name.name
        return "$root[[${
            functionType.arguments.joinToString(separator = ", ") { it.pythonTypeRepresentation() }
        }], ${functionType.returnValue.pythonTypeRepresentation()}]"
    }
}

// Special Python annotations
object PythonAnyTypeDescription : PythonSpecialAnnotation(pythonAnyName) {
    override fun getMemberByName(storage: PythonTypeStorage, type: Type, name: String): PythonAttribute {
        return PythonAttribute(name, pythonAnyType)
    }
}

object PythonNoneTypeDescription : PythonSpecialAnnotation(pythonNoneName) {
    // TODO: override getNamedMembers and/or getMemberByName
}

object PythonUnionTypeDescription : PythonSpecialAnnotation(pythonUnionName) {
    override fun getMemberByName(storage: PythonTypeStorage, type: Type, name: String): PythonAttribute? {
        val children = type.parameters.mapNotNull {
            it.getPythonAttributeByName(storage, name)?.type
        }
        return if (children.isEmpty())
            null
        else
            PythonAttribute(
                name = name,
                type = createPythonUnionType(children)
            )
    }

    override fun getAnnotationParameters(type: Type): List<Type> = type.parameters
}

object PythonOverloadTypeDescription : PythonSpecialAnnotation(overloadName) {
    override fun getAnnotationParameters(type: Type): List<Type> = type.parameters
    // TODO: override getMemberByName
}

object PythonTupleTypeDescription : PythonSpecialAnnotation(pythonTupleName) {
    override fun getAnnotationParameters(type: Type): List<Type> = castToCompatibleTypeApi(type).parameters
    // TODO: getMemberByName and/or getNamedMembers
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
    memberNames: List<String>,
    isAbstract: Boolean,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(
        numberOfParameters,
        PythonConcreteCompositeTypeDescription(name, memberNames, isAbstract),
        initialization
    )

fun createPythonProtocol(
    name: Name,
    numberOfParameters: Int,
    memberNames: List<String>,
    protocolMemberNames: List<String>,
    initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
): CompositeType =
    CompositeTypeCreator.create(
        numberOfParameters,
        PythonProtocolDescription(name, memberNames, protocolMemberNames),
        initialization
    )

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

private fun initTypeVar(param: TypeParameter) {
    param.meta = PythonTypeVarDescription(
        Name(emptyList(), ""), // TODO: name?
        PythonTypeVarDescription.Variance.INVARIANT,
        PythonTypeVarDescription.ParameterKind.WithUpperBound
    )
}

private fun substituteMembers(origin: Type, members: List<Type>): Type =
    DefaultSubstitutionProvider.substitute(
        origin,
        (origin.parameters.map { it as TypeParameter } zip members).associate { it }
    )

private fun createTypeWithMembers(description: PythonTypeDescription, members: List<Type>): Type {
    val origin = TypeCreator.create(members.size, description) {
        it.parameters.forEach(::initTypeVar)
    }
    return substituteMembers(origin, members)
}