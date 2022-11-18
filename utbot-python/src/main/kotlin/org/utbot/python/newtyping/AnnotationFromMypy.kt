package org.utbot.python.newtyping

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.python.newtyping.general.*

fun readMypyAnnotationStorage(jsonWithAnnotations: String, initObject: Boolean = true): MypyAnnotationStorage {
    val result = jsonAdapter.fromJson(jsonWithAnnotations) ?: error("Couldn't parse json with mypy annotations")
    if (initObject)
        result.definitions["builtins"]?.let { module ->
            module["object"]?.let { builtinsObject = it.annotation.asUtBotType }
        }
    return result
}

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
    .add(PythonTypeVarDescription.Variance::class.java, EnumJsonAdapter.create(PythonTypeVarDescription.Variance::class.java))
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
    val asUtBotType: Type
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

sealed class PythonAnnotationNode {
    @Transient lateinit var storage: MypyAnnotationStorage
    open val children: List<MypyAnnotation> = emptyList()
    abstract fun initializeType(): Type
}

sealed class CompositeAnnotationNode(
    val module: String,
    val simpleName: String,
    val names: Map<String, Definition>,
    val typeVars: List<MypyAnnotation>,
    val bases: List<MypyAnnotation>
): PythonAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + names.values.map { it.annotation } + typeVars + bases
    fun getInitData(self: CompositeTypeCreator.Original): CompositeTypeCreator.InitializationData {
        storage.nodeToUtBotType[this] = self
        (typeVars zip self.parameters).forEach { (node, typeParam) ->
            val typeVar = node.node as TypeVarNode
            storage.nodeToUtBotType[typeVar] = typeParam
            typeParam.meta = PythonTypeVarDescription(Name(emptyList(), typeVar.varName), typeVar.variance, typeVar.kind)
            typeParam.constraints = typeVar.constraints
        }
        val members = names.values.mapNotNull { def ->
            (def as? VarDefinition)?.annotation?.asUtBotType  // for now ignore inner types
        }
        val baseTypes = bases.map { it.asUtBotType }
        return CompositeTypeCreator.InitializationData(members, baseTypes)
    }
}

class ConcreteAnnotation(
    module: String,
    simpleName: String,
    names: Map<String, Definition>,
    typeVars: List<MypyAnnotation>,
    bases: List<MypyAnnotation>
): CompositeAnnotationNode(module, simpleName, names, typeVars, bases) {
    override fun initializeType(): Type {
        assert(storage.nodeToUtBotType[this] == null)
        return createPythonConcreteCompositeType(
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
    module: String,
    simpleName: String,
    names: Map<String, Definition>,
    typeVars: List<MypyAnnotation>,
    bases: List<MypyAnnotation>
): CompositeAnnotationNode(module, simpleName, names, typeVars, bases) {
    override fun initializeType(): Type {
        return createPythonProtocol(
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
    val typeVars: List<String>,
    val isClass: Boolean = false,
    val isStatic: Boolean = false
): PythonAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + positional + listOf(returnType)
    override fun initializeType(): Type {
        return createPythonCallableType(
            typeVars.size,
            positional.map { PythonCallableTypeDescription.ArgKind.Positional },
            isClass,
            isStatic
        ) { self ->
            storage.nodeToUtBotType[this] = self
            (typeVars zip self.parameters).forEach { (nodeId, typeParam) ->
                val typeVar = storage.nodeStorage[nodeId] as TypeVarNode
                storage.nodeToUtBotType[typeVar] = typeParam
                typeParam.meta = PythonTypeVarDescription(Name(emptyList(), typeVar.varName), typeVar.variance, typeVar.kind)
                typeParam.constraints = typeVar.constraints
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
    var upperBound: MypyAnnotation?,
    val def: String,
    val variance: PythonTypeVarDescription.Variance
): PythonAnnotationNode() {
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
): PythonAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): Type {
        return createPythonTupleType(items.map { it.asUtBotType })
    }
}

class PythonAny: PythonAnnotationNode() {
    override fun initializeType(): Type {
        return pythonAnyType
    }
}

//class PythonLiteral: PythonAnnotationNode("typing", "Literal", AnnotationType.Literal)

class PythonUnion(
    val items: List<MypyAnnotation>
): PythonAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): Type {
        return createPythonUnionType(items.map { it.asUtBotType })
    }
}

class PythonNoneType: PythonAnnotationNode() {
    override fun initializeType(): Type {
        return pythonNoneType
    }
}

class OverloadedFunction(
    val items: List<MypyAnnotation>
): PythonAnnotationNode() {
    override val children: List<MypyAnnotation>
        get() = super.children + items
    override fun initializeType(): Type {
        return createOverload(items.map { it.asUtBotType })
    }
}

class UnknownAnnotationNode: PythonAnnotationNode() {
    override fun initializeType(): Type {
        return pythonAnyType
    }
}

fun main() {
    val sample = MypyAnnotation::class.java.getResource("/subtypes_sample.json")!!.readText()
    val storage = readMypyAnnotationStorage(sample)
    val func = storage.definitions["subtypes"]!!["func_for_P"]!!.annotation.asUtBotType
    println((func.meta as PythonCallableTypeDescription).isStaticMethod)
    /*
    val int = storage.definitions["builtins"]!!["int"]!!.annotation.asUtBotType
    val obj = storage.definitions["builtins"]!!["object"]!!.annotation.asUtBotType
    val counter = storage.definitions["collections"]!!["Counter"]!!.annotation.asUtBotType
    println((counter.pythonDescription() as PythonCompositeTypeDescription).mro(counter).map { it.pythonDescription().name.name })
    println(int.pythonDescription().getMemberByName(int, "__init__"))
    println(counter.pythonDescription().getMemberByName(counter, "__init__"))
    println((int.pythonDescription() as PythonCompositeTypeDescription).mro(int).map { it.pythonDescription().name.name })
    println(obj.getPythonAttributes())
    println((obj.pythonDescription() as PythonCompositeTypeDescription).getMemberByName(obj, "__init__"))
    var cnt = 0
    val types = mutableSetOf<Type>()
    storage.definitions["builtins"]!!.forEach { (name, def) ->
        val type = def.annotation.asUtBotType
        val type1 = DefaultSubstitutionProvider.substitute(type, emptyMap())
        assert(type != type1)
    }
    storage.definitions.forEach { (_, contents) ->
        contents.forEach { (_, annotation) ->
            cnt += 1
            types.add(annotation.annotation.asUtBotType)
        }
    }
    print(cnt)
    var cnt1 = 0
    println(
        measureTimeMillis {
            types.forEach { a ->
                types.forEach { b ->
                    cnt1 += if (PythonTypeWrapperForComparison(a) == PythonTypeWrapperForComparison(b)) 1 else 0
                }
            }
        }
    )
    assert(cnt == cnt1)
     */
}