package org.utbot.python.newtyping.mypy

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.*

class MypyAnnotation(
    val nodeId: String,
    val args: List<MypyAnnotation>? = null
) {
    var initialized = false
    @Transient lateinit var storage: MypyInfoBuild
    val node: MypyAnnotationNode
        get() = storage.nodeStorage[nodeId]!!
    val asUtBotType: UtType
        get() {
            assert(initialized)
            val origin = storage.getUtBotTypeOfNode(node)
            if (args != null) {
                assert(origin.parameters.size == args.size)
                assert(origin.parameters.all { it is TypeParameter })
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
            val typeVar = node.node as TypeVarNode
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
        assert(storage.nodeToUtBotType[this] == null)
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
                val typeVar = storage.nodeStorage[nodeId] as TypeVarNode
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
    override fun initializeType() =
        error("Initialization of TypeVar must be done in defining class or function." +
                " TypeVar name: $varName, def_id: $def")
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

val annotationAdapter: PolymorphicJsonAdapterFactory<MypyAnnotationNode> =
    PolymorphicJsonAdapterFactory.of(MypyAnnotationNode::class.java, "type")
        .withSubtype(ConcreteAnnotation::class.java, AnnotationType.Concrete.name)
        .withSubtype(Protocol::class.java, AnnotationType.Protocol.name)
        .withSubtype(TypeVarNode::class.java, AnnotationType.TypeVar.name)
        .withSubtype(OverloadedFunction::class.java, AnnotationType.Overloaded.name)
        .withSubtype(FunctionNode::class.java, AnnotationType.Function.name)
        .withSubtype(PythonAny::class.java, AnnotationType.Any.name)
        //.withSubtype(PythonLiteral::class.java, AnnotationType.Literal.name)
        .withSubtype(PythonUnion::class.java, AnnotationType.Union.name)
        .withSubtype(PythonTuple::class.java, AnnotationType.Tuple.name)
        .withSubtype(PythonNoneType::class.java, AnnotationType.NoneType.name)
        .withSubtype(TypeAliasNode::class.java, AnnotationType.TypeAlias.name)
        .withSubtype(UnknownAnnotationNode::class.java, AnnotationType.Unknown.name)

object MypyAnnotations {

    data class MypyReportLine(
        val line: Int,
        val type: String,
        val message: String,
        val file: String
    )

}