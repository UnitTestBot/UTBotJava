package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.common.asPathToFile
import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.plugin.api.visible.UtStreamConsumingException
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.*
import java.util.stream.BaseStream

/**
 * Represents common interface for model constructors.
 */
interface UtModelConstructorInterface {
    /**
     * Constructs a UtModel from a concrete [value] with a specific [classId].
     */
    fun construct(value: Any?, classId: ClassId): UtModel
}

/**
 * Constructs models from concrete values.
 *
 * Uses reflection to traverse fields recursively ignoring static final fields. Also uses object->constructed model
 * reference-equality cache.
 *
 * @param objectToModelCache cache used for the model construction with respect to stateBefore. For each object, it first
 * @param compositeModelStrategy decides whether we should construct a composite model for a certain value or not.
 * @param maxDepth determines max depth for composite and assemble model nesting
 * searches in [objectToModelCache] for [UtReferenceModel.id].
 */
class UtModelConstructor(
    private val objectToModelCache: IdentityHashMap<Any, UtModel>,
    private val idGenerator: StateBeforeAwareIdGenerator,
    private val utModelWithCompositeOriginConstructorFinder: (ClassId) -> UtModelWithCompositeOriginConstructor?,
    private val compositeModelStrategy: UtCompositeModelStrategy = AlwaysConstructStrategy,
    private val maxDepth: Long = DEFAULT_MAX_DEPTH
) : UtModelConstructorInterface {
    private val constructedObjects = IdentityHashMap<Any, UtModel>()

    companion object {
        private const val DEFAULT_MAX_DEPTH = 7L

        fun createOnlyUserClassesConstructor(
            pathsToUserClasses: Set<String>,
            utModelWithCompositeOriginConstructorFinder: (ClassId) -> UtModelWithCompositeOriginConstructor?
        ): UtModelConstructor {
            val cache = IdentityHashMap<Any, UtModel>()
            val strategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(
                pathsToUserClasses, cache
            )
            return UtModelConstructor(
                objectToModelCache = cache,
                idGenerator = StateBeforeAwareIdGenerator(allPreExistingModels = emptySet()),
                utModelWithCompositeOriginConstructorFinder = utModelWithCompositeOriginConstructorFinder,
                compositeModelStrategy = strategy
            )
        }
    }

    private fun computeUnusedIdAndUpdate(): Int = idGenerator.createId()

    private fun handleId(value: Any): Int {
        return objectToModelCache[value]?.let { (it as? UtReferenceModel)?.id } ?: computeUnusedIdAndUpdate()
    }

    private val proxyLambdaSubstring = "$\$Lambda$"

    private fun isProxyLambda(value: Any?): Boolean {
        if (value == null) {
            return false
        }
        return proxyLambdaSubstring in value::class.java.name
    }

    private fun constructFakeLambda(value: Any, classId: ClassId): UtLambdaModel {
        val baseClassName = value::class.java.name.substringBefore(proxyLambdaSubstring)
        val baseClass = utContext.classLoader.loadClass(baseClassName).id
        return UtLambdaModel.createFake(handleId(value), classId, baseClass)
    }

    private fun isProxy(value: Any?): Boolean =
        value != null && Proxy.isProxyClass(value::class.java)

    /**
     * Using `UtAssembleModel` for dynamic proxies helps to avoid exceptions like
     * `java.lang.ClassNotFoundException: jdk.proxy3.$Proxy184` during code generation.
     */
    private fun constructProxy(value: Any, classId: ClassId): UtAssembleModel {
        val newProxyInstanceExecutableId = java.lang.reflect.Proxy::newProxyInstance.executableId

        // we don't want to construct deep models for invocationHandlers, since they can be quite large
        val argsRemainingDepth = 0L

        val classLoader = UtAssembleModel(
            id = computeUnusedIdAndUpdate(),
            classId = newProxyInstanceExecutableId.parameters[0],
            modelName = "systemClassLoader",
            instantiationCall = UtExecutableCallModel(
                instance = null,
                executable = ClassLoader::getSystemClassLoader.executableId,
                params = emptyList()
            )
        )
        val interfaces = construct(
            value::class.java.interfaces,
            newProxyInstanceExecutableId.parameters[1],
            remainingDepth = argsRemainingDepth
        )
        val invocationHandler = construct(
            Proxy.getInvocationHandler(value),
            newProxyInstanceExecutableId.parameters[2],
            remainingDepth = argsRemainingDepth
        )

        return UtAssembleModel(
            id = handleId(value),
            classId = classId,
            modelName = "dynamicProxy",
            instantiationCall = UtExecutableCallModel(
                instance = null,
                executable = newProxyInstanceExecutableId,
                params = listOf(classLoader, interfaces, invocationHandler)
            )
        )
    }

    /**
     * Constructs a UtModel from a concrete [value] with a specific [classId]. The result can be a [UtAssembleModel]
     * as well.
     *
     * Handles cache on stateBefore values.
     */
    override fun construct(value: Any?, classId: ClassId): UtModel =
        construct(value, classId, maxDepth)

    private fun construct(value: Any?, classId: ClassId, remainingDepth: Long): UtModel {
        objectToModelCache[value]?.let { model ->
            if (model is UtLambdaModel) {
                return model
            }
        }
        if (isProxyLambda(value)) {
            return constructFakeLambda(value!!, classId)
        }
        if (isProxy(value)) {
            return constructProxy(value!!, classId)
        }
        return when (value) {
            null -> UtNullModel(classId)
            is Unit -> UtVoidModel
            is Byte,
            is Short,
            is Char,
            is Int,
            is Long,
            is Float,
            is Double,
            is Boolean -> {
                if (classId.isPrimitive) UtPrimitiveModel(value)
                else constructFromAny(value, classId, remainingDepth)
            }

            is ByteArray -> constructFromByteArray(value, remainingDepth)
            is ShortArray -> constructFromShortArray(value, remainingDepth)
            is CharArray -> constructFromCharArray(value, remainingDepth)
            is IntArray -> constructFromIntArray(value, remainingDepth)
            is LongArray -> constructFromLongArray(value, remainingDepth)
            is FloatArray -> constructFromFloatArray(value, remainingDepth)
            is DoubleArray -> constructFromDoubleArray(value, remainingDepth)
            is BooleanArray -> constructFromBooleanArray(value, remainingDepth)
            is Array<*> -> constructFromArray(value, remainingDepth)
            is Enum<*> -> constructFromEnum(value)
            is Class<*> -> constructFromClass(value)
            is BaseStream<*, *> -> constructFromStream(value)
            else -> constructFromAny(value, classId, remainingDepth)
        }
    }

    fun constructMock(instance: Any, classId: ClassId, mocks: Map<MethodId, List<Any?>>): UtModel =
        constructedObjects.getOrElse(instance) {
            val utModel = UtCompositeModel(
                handleId(instance),
                classId,
                isMock = true,
                mocks = mocks.mapValuesTo(mutableMapOf()) { (method, values) ->
                    values.map { construct(it, method.returnType) }
                }
            )
            constructedObjects[instance] = utModel
            utModel
        }

    // Q: Is there a way to get rid of duplicated code?

    private fun constructFromDoubleArray(array: DoubleArray, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toDouble()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, doubleClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromFloatArray(array: FloatArray, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toFloat()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, floatClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromLongArray(array: LongArray, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toLong()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, longClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromIntArray(array: IntArray, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel = UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, intClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromCharArray(array: CharArray, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toChar()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, charClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromShortArray(array: ShortArray, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toShort()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, shortClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromByteArray(array: ByteArray, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toByte()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, byteClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromBooleanArray(array: BooleanArray, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(false), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, booleanClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromArray(array: Array<*>, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtNullModel(objectClassId), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, objectClassId, remainingDepth - 1)
            }
            utModel
        }

    private fun constructFromEnum(enum: Enum<*>): UtModel =
        constructedObjects.getOrElse(enum) {
            val utModel = UtEnumConstantModel(handleId(enum), enum::class.java.id, enum)
            constructedObjects[enum] = utModel
            utModel
        }

    private fun constructFromClass(clazz: Class<*>): UtModel =
        constructedObjects.getOrElse(clazz) {
            val utModel = UtClassRefModel(handleId(clazz), clazz::class.java.id, clazz.id)
            constructedObjects[clazz] = utModel
            utModel
        }

    private fun constructFromStream(stream: BaseStream<*, *>): UtModel =
        constructedObjects.getOrElse(stream) {
            val streamConstructor = findStreamConstructor(stream)

            try {
                streamConstructor.constructModelWithCompositeOrigin(this, stream, valueToClassId(stream), handleId(stream)) {
                    constructedObjects[stream] = it
                }
            } catch (e: Exception) {
                // An exception occurs during consuming of the stream -
                // remove the constructed object and throw this exception as a result
                constructedObjects.remove(stream)
                throw UtStreamConsumingException(e)
            }
        }

    /**
     * First tries to construct UtAssembleModel. If failure, constructs UtCompositeModel.
     */
    private fun constructFromAny(value: Any, classId: ClassId, remainingDepth: Long): UtModel =
        constructedObjects.getOrElse(value) {
            tryConstructCustomModel(value, remainingDepth)
                ?: findEqualValueOfWellKnownType(value)
                    ?.takeIf { (_, replacementClassId) -> replacementClassId isSubtypeOf classId }
                    ?.let { (replacement, replacementClassId) ->
                        // right now replacements only work with `UtAssembleModel`
                        (tryConstructCustomModel(replacement, remainingDepth) as? UtAssembleModel)
                            ?.copy(classId = replacementClassId)
                    }
                ?: constructCompositeModel(value, remainingDepth)
        }

    private fun findEqualValueOfWellKnownType(value: Any): Pair<Any, ClassId>? = runCatching {
        when (value) {
            is List<*> -> ArrayList(value) to listClassId
            is Set<*> -> LinkedHashSet(value) to setClassId
            is Map<*, *> -> LinkedHashMap(value) to mapClassId
            else -> null
        }
    }.getOrNull()

    /**
     * Constructs custom UtModel but does it only for predefined list of classes.
     *
     * Uses runtime class of [value].
     */
    private fun tryConstructCustomModel(value: Any, remainingDepth: Long): UtModel? =
        utModelWithCompositeOriginConstructorFinder(value::class.java.id)?.let { modelConstructor ->
            try {
                modelConstructor.constructModelWithCompositeOrigin(
                    internalConstructor = this.withMaxDepth(remainingDepth - 1),
                    value = value,
                    valueClassId = valueToClassId(value),
                    id = handleId(value),
                ) {
                    constructedObjects[value] = it
                }
            } catch (e: Exception) { // If UtAssembleModel constructor failed, we need to remove model and return null
                constructedObjects.remove(value)
                null
            }
        }

    /**
     * Constructs UtCompositeModel.
     *
     * Uses runtime javaClass to collect ALL fields, except final static fields, and builds this model recursively.
     */
    private fun constructCompositeModel(value: Any, remainingDepth: Long): UtCompositeModel {
        // value can be mock only if it was previously constructed from UtCompositeModel
        val isMock = objectToModelCache[value]?.isMockModel() ?: false

        val javaClazz = if (isMock) objectToModelCache.getValue(value).classId.jClass else value::class.java
        if (remainingDepth <= 0 || !compositeModelStrategy.shouldConstruct(value, javaClazz)) {
            return UtCompositeModel(
                handleId(value),
                javaClazz.id,
                isMock,
                fields = mutableMapOf() // we don't want to construct any further fields.
            )
        }

        val fields = mutableMapOf<FieldId, UtModel>()
        val utModel = UtCompositeModel(handleId(value), javaClazz.id, isMock, fields)
        constructedObjects[value] = utModel
        generateSequence(javaClazz) { it.superclass }.forEach { clazz ->
            val allFields = clazz.declaredFields
            allFields
                .asSequence()
                .filter { !(Modifier.isFinal(it.modifiers) && Modifier.isStatic(it.modifiers)) } // TODO: what about static final fields?
                .filterNot { it.fieldId.isInaccessibleViaReflection }
                .forEach { it.withAccessibility { fields[it.fieldId] = construct(it.get(value), it.type.id, remainingDepth - 1) } }
        }
        return utModel
    }

    private fun withMaxDepth(newMaxDepth: Long) = object : UtModelConstructorInterface {
        override fun construct(value: Any?, classId: ClassId): UtModel =
            construct(value, classId, newMaxDepth)
    }
}

/**
 * Decides, should we construct a UtCompositeModel from a value or not.
 */
interface UtCompositeModelStrategy {
    fun shouldConstruct(value: Any, clazz: Class<*>): Boolean
}

internal object AlwaysConstructStrategy : UtCompositeModelStrategy {
    override fun shouldConstruct(value: Any, clazz: Class<*>): Boolean = true
}

/**
 * This class constructs only user classes or values which are already in [objectToModelCache].
 *
 * [objectToModelCache] is a cache which we build in the time of creating concrete values from [UtModel]s.
 */
internal class ConstructOnlyUserClassesOrCachedObjectsStrategy(
    private val userDependencyPaths: Set<String>,
    private val objectToModelCache: IdentityHashMap<Any, UtModel>
) : UtCompositeModelStrategy {
    /**
     * Check whether [clazz] is a user class or [value] is in cache.
     */
    override fun shouldConstruct(value: Any, clazz: Class<*>): Boolean =
        isUserClass(clazz) || value in objectToModelCache

    private fun isUserClass(clazz: Class<*>): Boolean =
        clazz.protectionDomain.codeSource?.let { it.location.path.asPathToFile() in userDependencyPaths } ?: false

}