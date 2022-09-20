package org.utbot.engine

import org.utbot.common.unreachableBranch
import org.utbot.engine.overrides.collections.AssociativeArray
import org.utbot.engine.overrides.collections.UtArrayList
import org.utbot.engine.overrides.collections.UtGenericAssociative
import org.utbot.engine.overrides.collections.UtGenericStorage
import org.utbot.engine.overrides.collections.UtHashMap
import org.utbot.engine.overrides.collections.UtHashSet
import org.utbot.engine.overrides.collections.UtLinkedList
import org.utbot.engine.overrides.collections.UtLinkedListWithNullableCheck
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.select
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.z3.intValue
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.getIdOrThrow
import org.utbot.framework.util.graph
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.util.nextModelName
import soot.IntType
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.SootField
import soot.SootMethod

abstract class BaseOverriddenWrapper(protected val overriddenClassName: String) : WrapperInterface {
    val overriddenClass: SootClass = Scene.v().getSootClass(overriddenClassName)

    /**
     * Method returns list of [InvokeResult] if the specified [method] invocation results
     * should differ from the implementation of substituted method, as specified in [invoke]
     *
     * @see invoke
     */
    protected abstract fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>?

    /**
     * Substitute a method with the same subsignature from [overriddenClass] instead of specified [method].
     *
     * In details, if [overrideInvoke] returns non-null result, than it returns it.
     * Otherwise, it returns two [GraphResult] with body of specified method, if it can be found,
     * and with the body of substituted method with the same subsignature from [overriddenClass].
     *
     * Multiple GraphResults are returned because, we shouldn't substitute invocation of specified
     * that was called inside substituted method of object with the same address as specified [wrapper].
     * (For example UtArrayList.<init> invokes AbstractList.<init> that also leads to [Traverser.invoke],
     * and shouldn't be substituted with UtArrayList.<init> again). Only one GraphResult is valid, that is
     * guaranteed by contradictory to each other sets of constraints, added to them.
     */
    override fun Traverser.invoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult> {
        val methodResults = overrideInvoke(wrapper, method, parameters)
        if (methodResults != null) {
            return methodResults
        }

        if (method.isConstructor && method.declaringClass in hierarchy.ancestors(overriddenClass.id)) {
            return listOf(GraphResult(method.jimpleBody().graph()))
        }

        // We need to find either an override from the class (for example, implementation
        // of the method from the wrapper) or a method from its ancestors.
        // Note that the method from the ancestor might have substitutions as well.
        // I.e., it is `toString` method for `UtArrayList` that is defined in
        // `AbstractCollection` and has its own overridden implementation.
        val overriddenMethod = overriddenClass
            .findMethodOrNull(method.subSignature)
            ?.let { typeRegistry.findSubstitutionOrNull(it) ?: it }

        return if (overriddenMethod == null) {
            // No overridden method has been found, switch to concrete execution
            pathLogger.warn("Method ${overriddenClass.name}::${method.subSignature} not found, executing concretely")
            emptyList()
        } else {
            val jimpleBody = overriddenMethod.jimpleBody()
            val graphResult = GraphResult(jimpleBody.graph())
            listOf(graphResult)
        }
    }
}

/**
 * Wrapper for a particular [java.util.Collection] or [java.util.Map] or [java.util.stream.Stream].
 */
abstract class BaseContainerWrapper(containerClassName: String) : BaseOverriddenWrapper(containerClassName) {
    /**
     * Resolve [wrapper] to [UtAssembleModel] using [resolver].
     */
    override fun value(resolver: Resolver, wrapper: ObjectValue): UtAssembleModel = resolver.run {
        val addr = holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName(baseModelName)

        val parameterModels = resolveValueModels(wrapper)

        val classId = chooseClassIdWithConstructor(wrapper.type.sootClass.id)

        val instantiationCall = UtExecutableCallModel(
            instance = null,
            executable = constructorId(classId),
            params = emptyList()
        )

        UtAssembleModel(addr, classId, modelName, instantiationCall) {
            parameterModels.map { UtExecutableCallModel(this, modificationMethodId, it) }
        }
    }

    /**
     * Method returns list of parameter models that will be passed to [modificationMethodId]
     * while construction modification chain in [UtAssembleModel] for the specified [wrapper]
     */
    protected abstract fun Resolver.resolveValueModels(wrapper: ObjectValue): List<List<UtModel>>

    /**
     * Method returns the proper [ClassId] with default constructor for instantiation chain corresponding [classId]
     */
    protected abstract fun chooseClassIdWithConstructor(classId: ClassId): ClassId

    /**
     * MethodId that will be used in constructing the modification chain of [UtAssembleModel]
     */
    protected abstract val modificationMethodId: MethodId

    /**
     * base model name for resolved [UtAssembleModel]
     */
    protected abstract val baseModelName: String

    override fun toString() = "$overriddenClassName()"
}

abstract class BaseGenericStorageBasedContainerWrapper(containerClassName: String) : BaseContainerWrapper(containerClassName) {
    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? =
        when (method.signature) {
            UT_GENERIC_STORAGE_SET_EQUAL_GENERIC_TYPE_SIGNATURE -> {
                val equalGenericTypeConstraint = typeRegistry
                    .eqGenericSingleTypeParameterConstraint(parameters[0].addr, wrapper.addr)
                    .asHardConstraint()

                val methodResult = MethodResult(
                    SymbolicSuccess(voidValue),
                    equalGenericTypeConstraint
                )

                listOf(methodResult)
            }
            else -> null
        }

    override fun Resolver.resolveValueModels(wrapper: ObjectValue): List<List<UtModel>> {
        val elementDataFieldId = FieldId(overriddenClass.type.classId, "elementData")
        val arrayModel = collectFieldModels(wrapper.addr, overriddenClass.type)[elementDataFieldId] as? UtArrayModel

        return arrayModel?.let { constructValues(arrayModel, arrayModel.length) } ?: emptyList()
    }
}

/**
 * Auxiliary enum class for specifying an implementation for [ListWrapper], that it will use.
 */
enum class UtListClass {
    UT_ARRAY_LIST,
    UT_LINKED_LIST,
    UT_LINKED_LIST_WITH_NULLABLE_CHECK;

    val className: String
        get() = when (this) {
            UT_ARRAY_LIST -> UtArrayList::class.java.canonicalName
            UT_LINKED_LIST -> UtLinkedList::class.java.canonicalName
            UT_LINKED_LIST_WITH_NULLABLE_CHECK -> UtLinkedListWithNullableCheck::class.java.canonicalName
        }
}

/**
 * BaseCollectionWrapper that uses implementation of [UtArrayList], [UtLinkedList], or [UtLinkedListWithNullableCheck]
 * depending on specified [utListClass] parameter.
 *
 * This class is also used for wrapping [java.util.Queue], because [UtLinkedList] and [UtLinkedListWithNullableCheck]
 * both implement [java.util.Queue].
 *
 * At resolving stage ListWrapper is resolved to [UtAssembleModel].
 * This model is instantiated by constructor which is taken from the type from the passed [ReferenceValue] in [value]
 * function.
 *
 * Modification chain consists of consequent [java.util.Collection.add] methods
 * that are arranged to iterating order of list.
 */
class ListWrapper(private val utListClass: UtListClass) : BaseGenericStorageBasedContainerWrapper(utListClass.className) {
    /**
     * Chooses proper class for instantiation. Uses [java.util.ArrayList] instead of [java.util.List]
     * or [java.util.AbstractList].
     */
    override fun chooseClassIdWithConstructor(classId: ClassId) = when (classId.name) {
        "java.util.List", "java.util.AbstractList" -> java.util.ArrayList::class.id
        "java.util.Deque", "java.util.AbstractSequentialList" -> java.util.LinkedList::class.id
        else -> classId // TODO: actually we have to find not abstract class with constructor, but it's another story
    }

    override val modificationMethodId: MethodId = methodId(
        classId = java.util.Collection::class.id,
        name = "add",
        returnType = booleanClassId,
        arguments = arrayOf(objectClassId),
    )

    override val baseModelName = "list"
}

/**
 * BaseCollectionWrapper that uses implementation of [UtHashSet].
 *
 * At resolving stage SetWrapper is resolved to [UtAssembleModel].
 * This model is instantiated by [java.util.LinkedHashSet] constructor by default
 * or by [java.util.HashSet], if this type was set for this object wrapper explicitly.
 *
 * Modification chain consists of consequent [java.util.Set.add] methods that are arranged in
 * iterating order through [java.util.LinkedHashSet]. So, if there is an iterating
 * through [java.util.HashSet] in program and generated test depends on the order of
 * entries, then real behavior of generated test can differ from expected and undefined.
 */
class SetWrapper : BaseGenericStorageBasedContainerWrapper(UtHashSet::class.qualifiedName!!) {
    /**
     * Chooses proper class for instantiation. Uses [java.util.ArrayList] instead of [java.util.List]
     * or [java.util.AbstractList].
     */
    override fun chooseClassIdWithConstructor(classId: ClassId) = when (classId.name) {
        "java.util.Set", "java.util.AbstractSet" -> java.util.LinkedHashSet::class.id
        else -> classId // TODO: actually we have to find not abstract class with constructor, but it's another story
    }

    override val modificationMethodId: MethodId =
        methodId(
            classId = java.util.Set::class.id,
            name = "add",
            returnType = booleanClassId,
            arguments = arrayOf(objectClassId),
        )

    override val baseModelName: String = "set"
}

/**
 * BaseCollectionWrapper that uses implementation of [UtHashMap].
 *
 * At resolving stage MapWrapper is resolved to [UtAssembleModel].
 * This model is instantiated by [java.util.LinkedHashMap] constructor by default
 * or by [java.util.HashMap], if this type was set for this object wrapper explicitly.
 *
 * Modification chain consists of consistent [java.util.Map.put] methods that are arranged in
 * iterating order through [java.util.LinkedHashMap]. So, if there is iterating
 * through [java.util.HashMap] in program and generated test depends on the order of
 * entries, then real behavior of generated test can differ from expected and undefined.
 */
class MapWrapper : BaseContainerWrapper(UtHashMap::class.qualifiedName!!) {
    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? =
        when (method.signature) {
            UT_GENERIC_STORAGE_SET_EQUAL_GENERIC_TYPE_SIGNATURE -> listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    typeRegistry.eqGenericSingleTypeParameterConstraint(parameters[0].addr, wrapper.addr)
                        .asHardConstraint()
                )
            )
            UT_GENERIC_ASSOCIATIVE_SET_EQUAL_GENERIC_TYPE_SIGNATURE -> listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    typeRegistry.eqGenericTypeParametersConstraint(
                        parameters[0].addr,
                        wrapper.addr,
                        parameterSize = 2
                    ).asHardConstraint()
                )
            )
            else -> null
        }

    override fun Resolver.resolveValueModels(wrapper: ObjectValue): List<List<UtModel>> {
        val fieldModels = collectFieldModels(wrapper.addr, overriddenClass.type)
        val keyModels = fieldModels[overriddenClass.getFieldByName("keys").fieldId] as? UtArrayModel

        val valuesFieldId = overriddenClass.getFieldByName("values").fieldId
        val valuesCompositeModel = (fieldModels[valuesFieldId] as? UtCompositeModel) ?: return emptyList()
        val valuesStorageFieldId = FieldId(AssociativeArray::class.id, "storage")
        val valuesModel = valuesCompositeModel.fields[valuesStorageFieldId] as? UtArrayModel

        return if (valuesModel == null || keyModels == null) {
            emptyList()
        } else {
            constructKeysAndValues(keyModels, valuesModel, keyModels.length)
        }
    }

    /**
     * Chooses proper class for instantiation. Uses [java.util.ArrayList] instead of [java.util.List]
     * or [java.util.AbstractList].
     */
    override fun chooseClassIdWithConstructor(classId: ClassId) = when (classId.name) {
        "java.util.Map", "java.util.AbstractMap" -> java.util.LinkedHashMap::class.id
        else -> classId // TODO: actually we have to find not abstract class with constructor, but it's another story
    }

    override val modificationMethodId: MethodId =
        methodId(
            classId = java.util.Map::class.id,
            name = "put",
            returnType = objectClassId,
            arguments = arrayOf(objectClassId, objectClassId),
        )

    override val baseModelName: String = "map"
}

/**
 * Constructs collection values using underlying array model. If model is null model, returns list of nulls.
 */
internal fun constructValues(model: UtModel, size: Int): List<List<UtModel>> = when (model) {
    is UtArrayModel -> List(size) { listOf(model.stores[it] ?: model.constModel) }
    is UtNullModel -> {
        val elementClassId = model.classId.elementClassId
            ?: error("Class has to have elementClassId: ${model.classId}")
        List(size) { listOf(UtNullModel(elementClassId)) }
    }
    else -> unreachableBranch("$model is expected to be either UtArrayModel or UtNullModel")
}

/**
 * Constructs map keys and values using underlying array model. If model is null model, returns list of nulls.
 */
private fun constructKeysAndValues(keysModel: UtModel, valuesModel: UtModel, size: Int): List<List<UtModel>> =
    when {
        keysModel is UtNullModel || valuesModel is UtNullModel -> {
            val keyElementClassId = keysModel.classId.elementClassId
                ?: error("Class has to have elementClassId: ${keysModel.classId}")
            val valuesElementClassId = valuesModel.classId.elementClassId
                ?: error("Class has to have elementClassId: ${valuesModel.classId}")

            List(size) { listOf(UtNullModel(keyElementClassId), UtNullModel(valuesElementClassId)) }
        }
        keysModel is UtArrayModel && valuesModel is UtArrayModel -> {
            List(size) {
                keysModel.stores[it].let { model ->
                    val addr = model.getIdOrThrow()
                    // as we do not support generics for now, valuesModel.classId.elementClassId is unknown,
                    // but it can be known with generics support
                    val defaultValue = UtNullModel(valuesModel.classId.elementClassId ?: objectClassId)
                    // Missing address means Map precondition check was not traversed fully -
                    // keys were constructed with constraints but values were not.
                    // So, we use default null model for missing value.
                    listOf(model, valuesModel.stores[addr] ?: defaultValue)
                }
            }
        }
        else -> unreachableBranch("$keysModel and $valuesModel are expected to be either UtArrayModel or UtNullModel")
    }

private val UT_GENERIC_STORAGE_CLASS
    get() = Scene.v().getSootClass(UtGenericStorage::class.java.canonicalName)

internal val UT_GENERIC_STORAGE_SET_EQUAL_GENERIC_TYPE_SIGNATURE =
    UT_GENERIC_STORAGE_CLASS.getMethodByName(UtGenericStorage<*>::setEqualGenericType.name).signature

private val UT_GENERIC_ASSOCIATIVE_CLASS
    get() = Scene.v().getSootClass(UtGenericAssociative::class.java.canonicalName)

private val UT_GENERIC_ASSOCIATIVE_SET_EQUAL_GENERIC_TYPE_SIGNATURE =
    UT_GENERIC_ASSOCIATIVE_CLASS.getMethodByName(UtGenericAssociative<*, *>::setEqualGenericType.name).signature

val ARRAY_LIST_TYPE: RefType
    get() = Scene.v().getSootClass(java.util.ArrayList::class.java.canonicalName).type
val LINKED_LIST_TYPE: RefType
    get() = Scene.v().getSootClass(java.util.LinkedList::class.java.canonicalName).type

val LINKED_HASH_SET_TYPE: RefType
    get() = Scene.v().getSootClass(java.util.LinkedHashSet::class.java.canonicalName).type
val HASH_SET_TYPE: RefType
    get() = Scene.v().getSootClass(java.util.HashSet::class.java.canonicalName).type

val LINKED_HASH_MAP_TYPE: RefType
    get() = Scene.v().getSootClass(java.util.LinkedHashMap::class.java.canonicalName).type
val HASH_MAP_TYPE: RefType
    get() = Scene.v().getSootClass(java.util.HashMap::class.java.canonicalName).type

val STREAM_TYPE: RefType
    get() = Scene.v().getSootClass(java.util.stream.Stream::class.java.canonicalName).type

internal fun Traverser.getArrayField(
    addr: UtAddrExpression,
    wrapperClass: SootClass,
    field: SootField
): ArrayValue =
    createFieldOrMock(wrapperClass.type, addr, field, mockInfoGenerator = null) as ArrayValue

internal fun Traverser.getIntFieldValue(wrapper: ObjectValue, field: SootField): UtExpression {
    val chunkId = hierarchy.chunkIdForField(field.declaringClass.type, field)
    val descriptor = MemoryChunkDescriptor(chunkId, field.declaringClass.type, IntType.v())
    val array = memory.findArray(descriptor, MemoryState.CURRENT)
    return array.select(wrapper.addr)
}

internal fun Resolver.resolveIntField(wrapper: ObjectValue, field: SootField): Int {
    val chunkId = hierarchy.chunkIdForField(field.declaringClass.type, field)
    val descriptor = MemoryChunkDescriptor(chunkId, field.declaringClass.type, IntType.v())
    val array = findArray(descriptor, state)
    return holder.eval(array.select(wrapper.addr)).intValue()
}
