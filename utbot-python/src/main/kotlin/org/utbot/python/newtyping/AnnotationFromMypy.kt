package org.utbot.python.newtyping

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.python.newtyping.general.*

private val moshi = Moshi.Builder()
    .add(
        PolymorphicJsonAdapterFactory.of(PythonAnnotationNode::class.java, "type")
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
            .withSubtype(UnknownAnnotationNode::class.java, AnnotationType.Unknown.name)
    )
    .add(
        PolymorphicJsonAdapterFactory.of(Definition::class.java, "kind")
            .withSubtype(TypeDefinition::class.java, DefinitionType.Type.name)
            .withSubtype(VarDefinition::class.java, DefinitionType.Var.name)
    )
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val jsonAdapter = moshi.adapter(MypyAnnotationStorage::class.java)

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
    Unknown
}

enum class DefinitionType {
    Type,
    Var
}

sealed class Definition(
    val kind: DefinitionType,
    val annotation: MypyAnnotation
)
class TypeDefinition(
    annotation: MypyAnnotation
): Definition(DefinitionType.Type, annotation)
class VarDefinition(
    annotation: MypyAnnotation
): Definition(DefinitionType.Var, annotation)

class MypyAnnotationStorage(
    val nodeStorage: Map<String, PythonAnnotationNode>,
    val definitions: Map<String, Map<String, Definition>>
) {
    private fun initAnnotation(annotation: MypyAnnotation) {
        if (annotation.initialized)
            return
        annotation.storage = this
        annotation.initialized = true
        annotation.args?.forEach { initAnnotation(it) }
    }
    val nodeToUtBotType: MutableMap<PythonAnnotationNode, Type> = mutableMapOf()
    fun getUtBotTypeOfNode(node: PythonAnnotationNode): Type {
        val mem = nodeToUtBotType[node]
        if (mem != null)
            return mem
        val res = node.initializeType()
        nodeToUtBotType[node] = res
        return res
    }
    init {
        definitions.values.forEach { moduleMap ->
            moduleMap.values.forEach {
                initAnnotation(it.annotation)
            }
        }
        nodeStorage.values.forEach { node ->
            node.storage = this
            node.children.forEach { initAnnotation(it) }
        }
    }
}

class MypyAnnotation(
    val nodeId: String,
    val args: List<MypyAnnotation>? = null
) {
    var initialized = false
    @Transient lateinit var storage: MypyAnnotationStorage
    val node: PythonAnnotationNode
        get() = storage.nodeStorage[nodeId]!!
    val normalizedRepr: String
        get() {
            val children = args ?: return node.fullName
            return node.fullName + "[" + children.joinToString { it.normalizedRepr } + "]"
        }
    val asUtBotType: Type
        get() {
            assert(initialized)
            val origin = storage.getUtBotTypeOfNode(node)
            if (args != null) {
                assert(origin.parameters.size == args.size)
                assert(origin.parameters.all { it is TypeParameter })
                val argTypes = args.map { it.asUtBotType }
                return PythonTypeSubstitutionProvider.substitute(
                    origin,
                    (origin.parameters.map { it as TypeParameter } zip argTypes).toMap()
                )
            }
            return origin
    }
}

sealed class PythonAnnotationNode(
    open val module: String?,
    val simpleName: String,
    val type: AnnotationType
) {
    @Transient lateinit var storage: MypyAnnotationStorage
    open val fullName: String
        get() = if (module == null) simpleName else "$module.$simpleName"
    open val children: List<MypyAnnotation> = emptyList()
    abstract fun initializeType(): Type
}

sealed class CompositeAnnotationNode(
    override val module: String,
    simpleName: String,
    type: AnnotationType,
    val names: Map<String, Definition>,
    val typeVars: List<MypyAnnotation>,
    val bases: List<MypyAnnotation>
): PythonAnnotationNode(module, simpleName, type) {
    override val children: List<MypyAnnotation>
        get() = super.children + names.values.map { it.annotation } + typeVars + bases
    fun getInitData(self: CompositeTypeCreator.Original): CompositeTypeCreator.InitializationData {
        storage.nodeToUtBotType[this] = self
        (typeVars zip self.parameters).forEach { (node, typeParam) ->
            val typeVar = node.node as TypeVarNode
            storage.nodeToUtBotType[typeVar] = typeParam
            typeParam.meta = PythonTypeVarMetaData(typeVar.varName)
        }
        val members = names.values.mapNotNull { def ->
            (def as? VarDefinition)?.annotation?.asUtBotType  // for now ignore inner types
        }
        val baseTypes = bases.map { it.asUtBotType }
        val typeParameterConstraints = typeVars.map { (it.node as TypeVarNode).constraints }
        return CompositeTypeCreator.InitializationData(
            members,
            baseTypes,
            (self.parameters zip typeParameterConstraints).associate { it }
        )
    }
}

class ConcreteAnnotation(
    override val module: String,
    simpleName: String,
    names: Map<String, Definition>,
    typeVars: List<MypyAnnotation>,
    bases: List<MypyAnnotation>
): CompositeAnnotationNode(module, simpleName, AnnotationType.Concrete, names, typeVars, bases) {
    override fun initializeType(): Type {
        assert(storage.nodeToUtBotType[this] == null)
        return PythonConcreteCompositeTypeCreator.create(
            Name(module.split('.'), simpleName),
            typeVars.size,
            names.entries.mapNotNull {
                if (it.value is VarDefinition) it.key else null
            }
        ) { self -> getInitData(self) }
    }
}

class Protocol(
    val protocolMembers: List<String>,
    override val module: String,
    simpleName: String,
    names: Map<String, Definition>,
    typeVars: List<MypyAnnotation>,
    bases: List<MypyAnnotation>
): CompositeAnnotationNode(module, simpleName, AnnotationType.Protocol, names, typeVars, bases) {
    override fun initializeType(): Type {
        return PythonProtocolCreator.create(
            Name(module.split('.'), simpleName),
            typeVars.size,
            names.entries.mapNotNull {
                if (it.value is VarDefinition) it.key else null
            },
            protocolMembers
        ) { self -> getInitData(self) }
    }
}

class FunctionNode(
    val positional: List<MypyAnnotation>,  // for now ignore other argument kinds
    val returnType: MypyAnnotation,
    val typeVars: List<String>
): PythonAnnotationNode("typing", "Callable", AnnotationType.Function) {
    override val fullName: String
        get() =
            "${super.fullName}[[${positional.joinToString { it.normalizedRepr }}], ${returnType.normalizedRepr}]"
    override val children: List<MypyAnnotation>
        get() = super.children + positional + listOf(returnType)
    override fun initializeType(): Type {
        return PythonCallableCreator.create(
            typeVars.size,
            positional.map { PythonCallable.ArgKind.Positional }
        ) { self ->
            storage.nodeToUtBotType[this] = self
            (typeVars zip self.parameters).forEach { (nodeId, typeParam) ->
                val typeVar = storage.nodeStorage[nodeId] as TypeVarNode
                storage.nodeToUtBotType[typeVar] = typeParam
                typeParam.meta = PythonTypeVarMetaData(typeVar.varName)
            }
            FunctionTypeCreator.InitializationData(
                arguments = positional.map { it.asUtBotType },
                returnValue = returnType.asUtBotType
            )
        }
    }
}

class TypeVarNode(
    val varName: String,
    val values: List<MypyAnnotation>,
    val upperBound: MypyAnnotation?,
    val def: String
): PythonAnnotationNode("typing", "TypeVar", AnnotationType.TypeVar) {
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
}

class PythonTuple(
    val items: List<MypyAnnotation>
): PythonAnnotationNode("builtins", "tuple", AnnotationType.Tuple) {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): Type {
        return createPythonTupleType(items.map { it.asUtBotType })
    }
}

class PythonAny: PythonAnnotationNode("typing", "Any", AnnotationType.Any) {
    override fun initializeType(): Type {
        return pythonAnyType
    }
}

//class PythonLiteral: PythonAnnotationNode("typing", "Literal", AnnotationType.Literal)

class PythonUnion(
    val items: List<MypyAnnotation>
): PythonAnnotationNode("typing", "Union", AnnotationType.Union) {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): Type {
        return createPythonUnionType(items.map { it.asUtBotType })
    }
}

class PythonNoneType: PythonAnnotationNode(null, "None", AnnotationType.NoneType) {
    override fun initializeType(): Type {
        return pythonNoneType
    }
}

class OverloadedFunction(
    val items: List<MypyAnnotation>
): PythonAnnotationNode(null, "Overloaded", AnnotationType.Overloaded) {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): Type {
        return createOverloadedFunctionType(items.map { it.asUtBotType })
    }
}

class UnknownAnnotationNode: PythonAnnotationNode(null, "unknown", AnnotationType.Unknown) {
    override fun initializeType(): Type {
        return pythonAnyType
    }
}

fun main() {
    val sample = MypyAnnotation::class.java.getResource("/mypy/annotation_sample.txt")!!.readText()
    val obj = jsonAdapter.fromJson(sample)!!
    println((obj.definitions["builtins"]!!["set"]!!.annotation.node as ConcreteAnnotation).names["__xor__"]!!.annotation.normalizedRepr)
    println((obj.definitions["typing"]!!["Iterable"]!!.annotation.asUtBotType as PythonCompositeType).namedMembers)
    val classA = obj.definitions["annotation_tests"]!!["A"]!!.annotation.asUtBotType as PythonCompositeType
    println(classA.namedMembers)
    // should be PythonConcreteCompositeType (int)
    println(((classA.members[1] as PythonCallable).arguments[1] as PythonCompositeType).members[0])
    // should be int
    println(((obj.definitions["annotation_tests"]!!["square"]!!.annotation.asUtBotType as PythonCallable).arguments[0].parameters[0] as PythonCompositeType).name.name)
    val x = obj
}