package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.*
import org.utbot.python.newtyping.general.Name

sealed class PythonTypeDescription(name: Name) : TypeMetaDataWithName(name) {
    open fun castToCompatibleTypeApi(type: Type): Type = type
    open fun getNamedMembers(type: Type): List<PythonDefinition> = emptyList()  // direct members (without inheritance)
    open fun getAnnotationParameters(type: Type): List<Type> = emptyList()
    open fun getMemberByName(storage: PythonTypeStorage, type: Type, name: String): PythonDefinition? =
        // overridden for some types
        getNamedMembers(type).find { it.meta.name == name }
    open fun createTypeWithNewAnnotationParameters(like: Type, newParams: List<Type>): Type =  // overriden for Callable
        DefaultSubstitutionProvider.substituteAll(like.getOrigin(), newParams)
    open fun getTypeRepresentation(type: Type): String {  // overriden for Callable
        if (name.prefix == listOf("builtins") && name.name == "tuple") {
            return "${getTypeName()}[${type.parameters.first().pythonTypeRepresentation()}, ...]"
        }
        val root = getTypeName()
        val params = getAnnotationParameters(type)
        if (params.isEmpty())
            return root
        return "$root[${params.joinToString { it.pythonTypeRepresentation() }}]"
    }
    fun getTypeName(): String {
        return if (name.prefix.isEmpty())
            name.name
        else
            name.prefix.joinToString(".") + "." + name.name
    }
    fun getModules(type: Type): Set<String> {
        val cur = if (name.prefix.isNotEmpty())
            setOf(name.prefix.joinToString(separator = "."))
        else
            emptySet()
        return type.pythonAnnotationParameters().fold(cur) { acc, childType ->
            acc + childType.pythonModules()
        }
    }
}

sealed class PythonCompositeTypeDescription(
    name: Name,
    private val memberDescriptions: List<PythonDefinitionDescription>
): PythonTypeDescription(name) {
    override fun castToCompatibleTypeApi(type: Type): CompositeType {
        return type as? CompositeType
            ?: error("Got unexpected type PythonCompositeTypeDescription: $type")
    }

    override fun getNamedMembers(type: Type): List<PythonDefinition> {
        val compositeType = castToCompatibleTypeApi(type)
        assert(compositeType.members.size == memberDescriptions.size)
        return (memberDescriptions zip compositeType.members).map { (descr, typ) ->
            if (descr is PythonFuncItemDescription)
                PythonFunctionDefinition(descr, typ as FunctionType)
            else
                PythonDefinition(descr, typ)
        }
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

    override fun getMemberByName(storage: PythonTypeStorage, type: Type, name: String): PythonDefinition? {
        for (parent in mro(storage, type)) {
            val cur = parent.getPythonAttributes().find { it.meta.name == name }
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
    memberDescriptions: List<PythonDefinitionDescription>,
    val isAbstract: Boolean
) : PythonCompositeTypeDescription(name, memberDescriptions)

class PythonProtocolDescription(
    name: Name,
    memberDescriptions: List<PythonDefinitionDescription>,
    val protocolMemberNames: List<String>
) : PythonCompositeTypeDescription(name, memberDescriptions)

class PythonCallableTypeDescription(
    val argumentKinds: List<ArgKind>,
    val argumentNames: List<String?>  // like in mypy's CallableType: https://github.com/python/mypy/blob/master/mypy/types.py#L1672
): PythonTypeDescription(pythonCallableName) {
    val numberOfArguments = argumentKinds.size
    override fun castToCompatibleTypeApi(type: Type): FunctionType {
        return type as? FunctionType
            ?: error("Got unexpected type PythonCallableTypeDescription: $type")
    }

    override fun getNamedMembers(type: Type): List<PythonDefinition> {
        val functionType = castToCompatibleTypeApi(type)
        return listOf(PythonDefinition(PythonVariableDescription("__call__"), functionType))
    }

    override fun getAnnotationParameters(type: Type): List<Type> {
        val functionType = castToCompatibleTypeApi(type)
        return functionType.arguments + listOf(functionType.returnValue)
    }

    enum class ArgKind {
        ARG_POS,
        ARG_OPT,
        ARG_STAR,
        ARG_STAR_2,
        ARG_NAMED,
        ARG_NAMED_OPT
    }

    override fun createTypeWithNewAnnotationParameters(like: Type, newParams: List<Type>): Type {
        val args = newParams.dropLast(1)
        val returnValue = newParams.last()
        return createPythonCallableType(
            like.parameters.size,
            argumentKinds,
            argumentNames
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
    override fun getMemberByName(storage: PythonTypeStorage, type: Type, name: String): PythonDefinition {
        return PythonDefinition(PythonVariableDescription(name), pythonAnyType)
    }
}

object PythonNoneTypeDescription : PythonSpecialAnnotation(pythonNoneName) {
    // TODO: override getNamedMembers and/or getMemberByName
}

object PythonUnionTypeDescription : PythonSpecialAnnotation(pythonUnionName) {
    override fun getMemberByName(storage: PythonTypeStorage, type: Type, name: String): PythonDefinition? {
        val children = type.parameters.mapNotNull {
            it.getPythonAttributeByName(storage, name)?.type
        }
        return if (children.isEmpty())
            null
        else
            PythonDefinition(
                PythonVariableDescription(name),
                type = createPythonUnionType(children)
            )
    }

    override fun getAnnotationParameters(type: Type): List<Type> = type.parameters
}

object PythonOverloadTypeDescription : PythonSpecialAnnotation(overloadName) {
    override fun getAnnotationParameters(type: Type): List<Type> = type.parameters
    override fun getNamedMembers(type: Type): List<PythonDefinition> {
        return listOf(PythonDefinition(PythonVariableDescription("__call__"), type))
    }
}

object PythonTypeAliasDescription : PythonSpecialAnnotation(pythonTypeAliasName) {
    override fun castToCompatibleTypeApi(type: Type): CompositeType {
        return type as? CompositeType ?: error("Got unexpected type for PythonTypeAliasDescription: $type")
    }
    fun getInterior(type: Type): Type {
        val casted = castToCompatibleTypeApi(type)
        return casted.members.first()
    }
}

object PythonTupleTypeDescription : PythonSpecialAnnotation(pythonTupleName) {
    override fun getAnnotationParameters(type: Type): List<Type> = castToCompatibleTypeApi(type).parameters
    // TODO: getMemberByName and/or getNamedMembers
}

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

fun createTypeWithMembers(description: PythonTypeDescription, members: List<Type>): Type {
    val origin = TypeCreator.create(members.size, description) {
        it.parameters.forEach(::initTypeVar)
    }
    return substituteMembers(origin, members)
}