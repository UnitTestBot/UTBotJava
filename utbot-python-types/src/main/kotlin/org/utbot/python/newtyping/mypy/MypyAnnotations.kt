package org.utbot.python.newtyping.mypy

import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.*
import org.utbot.python.utils.CustomPolymorphicJsonAdapterFactory

class MypyAnnotation(
    val nodeId: String,
    val args: List<MypyAnnotation>? = null
) {
    var initialized = false
    @Transient lateinit var storage: MypyInfoBuild
    val node: MypyAnnotationNode
        get() {
            val result = storage.nodeStorage[nodeId]
            require(result != null) {
                "Required node is absent in storage: $nodeId"
            }
            return result
        }
    val asUtBotType: UtType
        get() {
            require(initialized)
            val origin = storage.getUtBotTypeOfNode(node)
            if (origin.pythonDescription() is PythonAnyTypeDescription)
                return origin
            if (args != null) {
                require(origin.parameters.size == args.size) {
                    "Bad arguments on ${origin.pythonTypeRepresentation()}. Expected ${origin.parameters.size} parameters but got ${args.size}"
                }
                require(origin.parameters.all { it is TypeParameter })
                val argTypes = args.map { it.asUtBotType }
                return DefaultSubstitutionProvider.substitute(
                    origin,
                    (origin.parameters.map { it as TypeParameter } zip argTypes).toMap()
                )
            }
            return origin
        }
}

sealed class MypyAnnotationNode {
    @Transient lateinit var storage: MypyInfoBuild
    open val children: List<MypyAnnotation> = emptyList()
    abstract fun initializeType(): UtType
}

sealed class CompositeAnnotationNode(
    val module: String,
    val simpleName: String,
    val members: List<MypyDefinition>,
    val typeVars: List<MypyAnnotation>,
    val bases: List<MypyAnnotation>
): MypyAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + members.map { it.type } + typeVars + bases
    fun getInitData(self: CompositeTypeCreator.Original): CompositeTypeCreator.InitializationData {
        storage.nodeToUtBotType[this] = self
        (typeVars zip self.parameters).forEach { (node, typeParam) ->
            val typeVar = node.node as? TypeVarNode
            require(typeVar != null) {
                "Did not construct type variable"
            }
            storage.nodeToUtBotType[typeVar] = typeParam
            typeParam.meta = PythonTypeVarDescription(Name(emptyList(), typeVar.varName), typeVar.variance, typeVar.kind)
            typeParam.constraints = typeVar.constraints
        }
        val membersForType = members.mapNotNull { def ->
            def.getUtBotDefinition()?.type  // for now ignore inner types
        }
        val baseTypes = bases.map { it.asUtBotType }
        return CompositeTypeCreator.InitializationData(membersForType, baseTypes)
    }
}


class ConcreteAnnotation(
    module: String,
    simpleName: String,
    members: List<MypyDefinition>,
    typeVars: List<MypyAnnotation>,
    bases: List<MypyAnnotation>,
    val isAbstract: Boolean
): CompositeAnnotationNode(module, simpleName, members, typeVars, bases) {
    override fun initializeType(): UtType {
        require(storage.nodeToUtBotType[this] == null)
        return createPythonConcreteCompositeType(
            Name(module.split('.'), simpleName),
            typeVars.size,
            members.mapNotNull { it.getUtBotDescription() },
            isAbstract
        ) { self -> getInitData(self) }
    }
}


class Protocol(
    val protocolMembers: List<String>,
    module: String,
    simpleName: String,
    members: List<MypyDefinition>,
    typeVars: List<MypyAnnotation>,
    bases: List<MypyAnnotation>
): CompositeAnnotationNode(module, simpleName, members, typeVars, bases) {
    override fun initializeType(): UtType {
        return createPythonProtocol(
            Name(module.split('.'), simpleName),
            typeVars.size,
            members.mapNotNull { it.getUtBotDescription() },
            protocolMembers
        ) { self -> getInitData(self) }
    }
}


class FunctionNode(
    val argTypes: List<MypyAnnotation>,  // for now ignore other argument kinds
    val returnType: MypyAnnotation,
    val typeVars: List<String>,
    val argKinds: List<PythonCallableTypeDescription.ArgKind>,
    var argNames: List<String?>,
): MypyAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + argTypes + listOf(returnType)
    override fun initializeType(): UtType {
        return createPythonCallableType(
            typeVars.size,
            argKinds,
            argNames
        ) { self ->
            storage.nodeToUtBotType[this] = self
            (typeVars zip self.parameters).forEach { (nodeId, typeParam) ->
                val typeVar = storage.nodeStorage[nodeId] as? TypeVarNode
                require(typeVar != null) {
                    "Did not construct type variable"
                }
                storage.nodeToUtBotType[typeVar] = typeParam
                typeParam.meta = PythonTypeVarDescription(Name(emptyList(), typeVar.varName), typeVar.variance, typeVar.kind)
                typeParam.constraints = typeVar.constraints
            }
            FunctionTypeCreator.InitializationData(
                arguments = argTypes.map { it.asUtBotType },
                returnValue = returnType.asUtBotType
            )
        }
    }
}


class TypeVarNode(
    val varName: String,
    val values: List<MypyAnnotation>,
    var upperBound: MypyAnnotation?,
    val def: String,
    val variance: PythonTypeVarDescription.Variance
): MypyAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + values + (upperBound?.let { listOf(it) } ?: emptyList())
    override fun initializeType(): UtType {
        /*error(
            "Initialization of TypeVar must be done in defining class or function." +
                    " TypeVar name: $varName, def_id: $def"
        )*/
        // this a rare and bad case:
        // https://github.com/sqlalchemy/sqlalchemy/blob/rel_2_0_20/lib/sqlalchemy/sql/sqltypes.py#L2091C5-L2091C23
        storage.nodeStorage[def]!!.initializeType()
        return storage.nodeToUtBotType[this] ?: error("Error while initializing TypeVar name: $varName, def_id: $def")
    }
    val constraints: Set<TypeParameterConstraint> by lazy {
        val upperBoundConstraint: Set<TypeParameterConstraint> =
            upperBound?.let { setOf(TypeParameterConstraint(upperBoundRelation, it.asUtBotType)) } ?: emptySet()
        values.map { TypeParameterConstraint(exactTypeRelation, it.asUtBotType) }.toSet() + upperBoundConstraint
    }
    val kind: PythonTypeVarDescription.ParameterKind
        get() {
            return if (values.isEmpty())
                PythonTypeVarDescription.ParameterKind.WithUpperBound
            else {
                upperBound = null
                PythonTypeVarDescription.ParameterKind.WithConcreteValues
            }
        }
}


class PythonTuple(
    val items: List<MypyAnnotation>
): MypyAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): UtType {
        return createPythonTupleType(items.map { it.asUtBotType })
    }
}

class PythonAny: MypyAnnotationNode() {
    override fun initializeType(): UtType {
        return pythonAnyType
    }
}

//class PythonLiteral: PythonAnnotationNode("typing", "Literal", AnnotationType.Literal)

class PythonUnion(
    val items: List<MypyAnnotation>
): MypyAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): UtType {
        return createPythonUnionType(items.map { it.asUtBotType })
    }
}

class PythonNoneType: MypyAnnotationNode() {
    override fun initializeType(): UtType {
        return pythonNoneType
    }
}

class OverloadedFunction(
    val items: List<MypyAnnotation>
): MypyAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): UtType {
        return createOverload(items.map { it.asUtBotType })
    }
}

class TypeAliasNode(val target: MypyAnnotation): MypyAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + target
    override fun initializeType(): UtType {
        return createPythonTypeAlias { self ->
            storage.nodeToUtBotType[this] = self
            target.asUtBotType
        }
    }
}

class UnknownAnnotationNode: MypyAnnotationNode() {
    override fun initializeType(): UtType {
        return pythonAnyType
    }
}

enum class AnnotationType {
    Concrete,
    Protocol,
    TypeVar,
    Overloaded,
    Function,
    Any,
    Literal,
    Union,
    Tuple,
    NoneType,
    TypeAlias,
    Unknown
}

val annotationAdapter = CustomPolymorphicJsonAdapterFactory(
    MypyAnnotationNode::class.java,
    contentLabel = "content",
    keyLabel = "type",
    mapOf(
        AnnotationType.Concrete.name to ConcreteAnnotation::class.java,
        AnnotationType.Protocol.name to Protocol::class.java,
        AnnotationType.TypeVar.name to TypeVarNode::class.java,
        AnnotationType.Overloaded.name to OverloadedFunction::class.java,
        AnnotationType.Function.name to FunctionNode::class.java,
        AnnotationType.Any.name to PythonAny::class.java,
        // .withSubtype(PythonLiteral::class.java, AnnotationType.Literal.name)
        AnnotationType.Union.name to PythonUnion::class.java,
        AnnotationType.Tuple.name to PythonTuple::class.java,
        AnnotationType.NoneType.name to PythonNoneType::class.java,
        AnnotationType.TypeAlias.name to TypeAliasNode::class.java,
        AnnotationType.Unknown.name to UnknownAnnotationNode::class.java
    )
)