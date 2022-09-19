package org.utbot.framework.concrete

import org.utbot.common.asPathToFile
import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.util.isInaccessibleViaReflection
import org.utbot.framework.util.valueToClassId
import java.lang.reflect.Modifier
import java.util.IdentityHashMap

/**
 * Represents common interface for model constructors.
 */
internal interface UtModelConstructorInterface {
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
 * searches in [objectToModelCache] for [UtReferenceModel.id].
 */
class UtModelConstructor(
    private val objectToModelCache: IdentityHashMap<Any, UtModel>,
    private val compositeModelStrategy: UtCompositeModelStrategy = AlwaysConstructStrategy
) : UtModelConstructorInterface {
    private val constructedObjects = IdentityHashMap<Any, UtModel>()

    private var unusedId = 0
    private val usedIds = objectToModelCache.values
        .filterIsInstance<UtReferenceModel>()
        .mapNotNull { it.id }
        .toMutableSet()

    private fun computeUnusedIdAndUpdate(): Int {
        while (unusedId in usedIds) {
            unusedId++
        }
        return unusedId.also { usedIds += it }
    }

    private fun handleId(value: Any): Int {
        return objectToModelCache[value]?.let { (it as? UtReferenceModel)?.id } ?: computeUnusedIdAndUpdate()
    }

    /**
     * Constructs a UtModel from a concrete [value] with a specific [classId]. The result can be a [UtAssembleModel]
     * as well.
     *
     * Handles cache on stateBefore values.
     */
    override fun construct(value: Any?, classId: ClassId): UtModel {
        objectToModelCache[value]?.let { model ->
            if (model is UtLambdaModel) {
                return model
            }
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
            is Boolean -> if (classId.isPrimitive) UtPrimitiveModel(value) else constructFromAny(value)
            is ByteArray -> constructFromByteArray(value)
            is ShortArray -> constructFromShortArray(value)
            is CharArray -> constructFromCharArray(value)
            is IntArray -> constructFromIntArray(value)
            is LongArray -> constructFromLongArray(value)
            is FloatArray -> constructFromFloatArray(value)
            is DoubleArray -> constructFromDoubleArray(value)
            is BooleanArray -> constructFromBooleanArray(value)
            is Array<*> -> constructFromArray(value)
            is Enum<*> -> constructFromEnum(value)
            is Class<*> -> constructFromClass(value)
            else -> constructFromAny(value)
        }
    }

    // Q: Is there a way to get rid of duplicated code?

    private fun constructFromDoubleArray(array: DoubleArray): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toDouble()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, doubleClassId)
            }
            utModel
        }

    private fun constructFromFloatArray(array: FloatArray): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toFloat()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, floatClassId)
            }
            utModel
        }

    private fun constructFromLongArray(array: LongArray): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toLong()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, longClassId)
            }
            utModel
        }

    private fun constructFromIntArray(array: IntArray): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel = UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, intClassId)
            }
            utModel
        }

    private fun constructFromCharArray(array: CharArray): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toChar()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, charClassId)
            }
            utModel
        }

    private fun constructFromShortArray(array: ShortArray): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toShort()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, shortClassId)
            }
            utModel
        }

    private fun constructFromByteArray(array: ByteArray): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(0.toByte()), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, byteClassId)
            }
            utModel
        }

    private fun constructFromBooleanArray(array: BooleanArray): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtPrimitiveModel(false), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, booleanClassId)
            }
            utModel
        }

    private fun constructFromArray(array: Array<*>): UtModel =
        constructedObjects.getOrElse(array) {
            val stores = mutableMapOf<Int, UtModel>()
            val utModel =
                UtArrayModel(handleId(array), array::class.java.id, array.size, UtNullModel(objectClassId), stores)
            constructedObjects[array] = utModel
            array.forEachIndexed { idx, value ->
                stores[idx] = construct(value, objectClassId)
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
            val utModel = UtClassRefModel(handleId(clazz), clazz::class.java.id, clazz)
            System.err.println("ClassRef: $clazz \t\tClassloader: ${clazz.classLoader}")
            constructedObjects[clazz] = utModel
            utModel
        }

    /**
     * First tries to construct UtAssembleModel. If failure, constructs UtCompositeModel.
     */
    private fun constructFromAny(value: Any): UtModel =
        constructedObjects.getOrElse(value) {
            tryConstructUtAssembleModel(value) ?: constructCompositeModel(value)
        }

    /**
     * Constructs UtAssembleModel but does it only for predefined list of classes.
     *
     * Uses runtime class of an object.
     */
    private fun tryConstructUtAssembleModel(value: Any): UtModel? =
        findUtAssembleModelConstructor(value::class.java.id)?.let { assembleConstructor ->
            try {
                assembleConstructor.constructAssembleModel(this, value, valueToClassId(value), handleId(value)) {
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
    private fun constructCompositeModel(value: Any): UtModel {
        // value can be mock only if it was previously constructed from UtCompositeModel
        val isMock = objectToModelCache[value]?.isMockModel() ?: false

        val javaClazz = if (isMock) objectToModelCache.getValue(value).classId.jClass else value::class.java
        if (!compositeModelStrategy.shouldConstruct(value, javaClazz)) {
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
                .forEach { it.withAccessibility { fields[it.fieldId] = construct(it.get(value), it.type.id) } }
        }
        return utModel
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