package org.utbot.engine.types

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.ArrayValue
import org.utbot.engine.ChunkId
import org.utbot.engine.Hierarchy
import org.utbot.engine.MemoryChunkDescriptor
import org.utbot.engine.ObjectValue
import org.utbot.engine.ReferenceValue
import org.utbot.engine.TypeStorage
import org.utbot.engine.appropriateClasses
import org.utbot.engine.baseType
import org.utbot.engine.findMockAnnotationOrNull
import org.utbot.engine.isAppropriate
import org.utbot.engine.isArtificialEntity
import org.utbot.engine.isInappropriate
import org.utbot.engine.isJavaLangObject
import org.utbot.engine.isLambda
import org.utbot.engine.isOverridden
import org.utbot.engine.isUtMock
import org.utbot.engine.makeArrayType
import org.utbot.engine.nullObjectAddr
import org.utbot.engine.numDimensions
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.mkAnd
import org.utbot.engine.pc.mkEq
import org.utbot.engine.rawType
import org.utbot.engine.wrapper
import org.utbot.engine.wrapperToClass
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.util.UTBOT_FRAMEWORK_API_VISIBLE_PACKAGE
import soot.ArrayType
import soot.IntType
import soot.NullType
import soot.PrimType
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.SootField
import soot.Type
import soot.VoidType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class TypeResolver(private val typeRegistry: TypeRegistry, private val hierarchy: Hierarchy) {

    fun findOrConstructInheritorsIncludingTypes(type: RefType) = typeRegistry.findInheritorsIncludingTypes(type) {
        hierarchy.inheritors(type.sootClass.id).mapTo(mutableSetOf()) { it.type }
    }

    fun findOrConstructAncestorsIncludingTypes(type: RefType) = typeRegistry.findAncestorsIncludingTypes(type) {
        hierarchy.ancestors(type.sootClass.id).mapTo(mutableSetOf()) { it.type }
    }

    /**
     * Finds all the inheritors for each type from the [types] and returns their intersection.
     *
     * Note: every type from the result satisfies [isAppropriate] condition.
     */
    fun intersectInheritors(types: Array<java.lang.reflect.Type>): Set<RefType> = intersectTypes(types) {
        findOrConstructInheritorsIncludingTypes(it)
    }

    /**
     * Finds all the ancestors for each type from the [types] and return their intersection.
     *
     * Note: every type from the result satisfies [isAppropriate] condition.
     */
    fun intersectAncestors(types: Array<java.lang.reflect.Type>): Set<RefType> = intersectTypes(types) {
        findOrConstructAncestorsIncludingTypes(it)
    }

    private fun intersectTypes(
        types: Array<java.lang.reflect.Type>,
        retrieveFunction: (RefType) -> Set<RefType>
    ): Set<RefType> {
        val allObjects = findOrConstructInheritorsIncludingTypes(OBJECT_TYPE)

        // TODO we do not support constructions like List<? extends T[][]> here, be aware of it
        // TODO JIRA:1446

        return types
            .map { classOrDefault(it.rawType.typeName) }
            .fold(allObjects) { acc, value -> acc.intersect(retrieveFunction(value)) }
            .filter { it.sootClass.isAppropriate }
            .toSet()
    }

    private fun classOrDefault(typeName: String): RefType =
        runCatching { Scene.v().getRefType(typeName) }.getOrDefault(OBJECT_TYPE)

    fun findFields(type: RefType) = typeRegistry.findFields(type) {
        hierarchy
            .ancestors(type.sootClass.id)
            .flatMap { it.fields }
    }

    /**
     * Returns given number of appropriate types that have the highest rating.
     *
     * @param types Collection of types to sort
     * @param take Number of types to take
     *
     * @see TypeRegistry.findRating
     * @see appropriateClasses
     */
    fun findTopRatedTypes(types: Collection<Type>, take: Int = Int.MAX_VALUE) =
        types.appropriateClasses()
            .sortedByDescending { type ->
                val baseType = if (type is ArrayType) type.baseType else type
                // primitive baseType has the highest possible rating
                if (baseType is RefType) typeRegistry.findRating(baseType) else Int.MAX_VALUE
            }
            .take(take)

    /**
     * Constructs a [TypeStorage] instance containing [type] as its most common type and
     * appropriate types from [possibleTypes] in its [TypeStorage.possibleConcreteTypes].
     *
     * @param type the most common type of the constructed type storage.
     * @param possibleTypes a list of types to be contained in the constructed type storage.
     *
     * @return constructed type storage.
     *
     * Note: [TypeStorage.possibleConcreteTypes] of the type storage returned by this method contains only
     * classes we can instantiate: there will be no interfaces, abstract or local classes.
     * If there are no such classes, [TypeStorage.possibleConcreteTypes] is an empty set.
     *
     * @see isAppropriate
     */
    fun constructTypeStorage(type: Type, possibleTypes: Collection<Type>): TypeStorage {
        val concretePossibleTypes = possibleTypes
            .map { (if (it is ArrayType) it.baseType else it) to it.numDimensions }
            .filterNot { (baseType, numDimensions) -> isInappropriateOrArrayOfMocks(numDimensions, baseType) }
            .mapTo(mutableSetOf()) { (baseType, numDimensions) ->
                if (numDimensions == 0) baseType else baseType.makeArrayType(numDimensions)
            }

        return TypeStorage.constructTypeStorageUnsafe(type, concretePossibleTypes).removeInappropriateTypes()
    }

    private fun isInappropriateOrArrayOfMocks(numDimensions: Int, baseType: Type?): Boolean {
        if (baseType !is RefType) {
            return false
        }

        val baseSootClass = baseType.sootClass

        // We don't want to have our wrapper's classes as a part of a regular TypeStorage instance
        // Note that we cannot have here 'isOverridden' since iterators of our wrappers are not wrappers
        if (wrapperToClass[baseType] != null) {
            return true
        }

        if (numDimensions == 0 && baseSootClass.isInappropriate) {
            // interface, abstract class, or mock could not be constructed
            return true
        }

        if (numDimensions > 0 && baseSootClass.findMockAnnotationOrNull != null) {
            // array of mocks could not be constructed, but array of interfaces or abstract classes could be
            return true
        }

        return false
    }

    /**
     * Constructs a [TypeStorage] instance for given [type].
     * Depending on [useConcreteType] it will or will not contain type's inheritors.
     *
     * @param type a type for which we want to construct type storage.
     * @param useConcreteType a boolean parameter to determine whether we want to include inheritors of the type or not.
     *
     * @return constructed type storage.
     *
     * Note: [TypeStorage.possibleConcreteTypes] of the type storage returned by this method contains only
     * classes we can instantiate: there will be no interfaces, abstract or local classes.
     * If there are no such classes, [TypeStorage.possibleConcreteTypes] is an empty set.
     *
     * @see isAppropriate
     */
    fun constructTypeStorage(type: Type, useConcreteType: Boolean): TypeStorage {
        // create a typeStorage with concreteType even if the type belongs to an interface or an abstract class
        if (useConcreteType) return TypeStorage.constructTypeStorageWithSingleType(type)

        val baseType = type.baseType

        val inheritors = if (baseType !is RefType) {
            setOf(baseType)
        } else {
            // use only 'appropriate' classes in the TypeStorage construction
            val allInheritors = findOrConstructInheritorsIncludingTypes(baseType)

            // if the type is ArrayType, we don't have to filter abstract classes and interfaces from the inheritors
            // because we still can instantiate, i.e., Number[].
            if (type is ArrayType) {
                allInheritors
            } else {
                allInheritors.filterTo(mutableSetOf()) { it.sootClass.isAppropriate }
            }
        }

        val extendedInheritors = if (baseType.isJavaLangObject()) inheritors + TypeRegistry.primTypes else inheritors

        val possibleTypes = when (type) {
            is RefType, is PrimType -> extendedInheritors
            is ArrayType -> when (baseType) {
                is RefType -> extendedInheritors.map { it.makeArrayType(type.numDimensions) }.toSet()
                else -> setOf(baseType.makeArrayType(type.numDimensions))
            }
            else -> error("Unexpected type $type")
        }

        return TypeStorage.constructTypeStorageUnsafe(type, possibleTypes).removeInappropriateTypes()
    }

    /**
     * Remove wrapper types, classes from the visible for Soot package and, if any other type is available, artificial entities.
     */
    private fun TypeStorage.removeInappropriateTypes(): TypeStorage {
        val leastCommonSootClass = (leastCommonType as? RefType)?.sootClass
        val keepArtificialEntities = leastCommonSootClass?.isArtificialEntity == true

        val appropriateTypes = possibleConcreteTypes.filter {
            // All not RefType should be included in the concreteTypes, e.g., arrays
            val sootClass = (it.baseType as? RefType)?.sootClass ?: return@filter true

            // All artificial entities except anonymous functions should be filtered out if we have another types
            if (sootClass.isArtificialEntity) {
                if (sootClass.isLambda) {
                    return@filter true
                }

                return@filter keepArtificialEntities
            }

            // All wrappers and classes from the visible for Soot package should be filtered out because they could not be instantiated
            workaround(WorkaroundReason.HACK) {
                if (leastCommonSootClass == OBJECT_TYPE && sootClass.isOverridden) {
                    return@filter false
                }

                if (sootClass.packageName == UTBOT_FRAMEWORK_API_VISIBLE_PACKAGE) {
                    return@filter false
                }
            }

            return@filter true
        }.toSet()

        return TypeStorage.constructTypeStorageUnsafe(leastCommonType, appropriateTypes)
    }

    /**
     * Constructs a nullObject with TypeStorage containing all the inheritors for the given type
     */
    fun nullObject(type: Type): ReferenceValue {
        val typeStorage = TypeStorage.constructTypeStorageWithSingleType(type)

        return when (type) {
            is RefType, is NullType, is VoidType -> ObjectValue(typeStorage, nullObjectAddr)
            is ArrayType -> ArrayValue(typeStorage, nullObjectAddr)
            else -> error("Unsupported nullType $type")
        }
    }

    fun downCast(arrayValue: ArrayValue, typeToCast: ArrayType): ArrayValue {
        val typesBeforeCast = findOrConstructInheritorsIncludingTypes(arrayValue.type.baseType as RefType)
        val typesAfterCast = findOrConstructInheritorsIncludingTypes(typeToCast.baseType as RefType)
        val possibleTypes = typesBeforeCast.filter { it in typesAfterCast }.map {
            it.makeArrayType(arrayValue.type.numDimensions)
        }

        return arrayValue.copy(typeStorage = constructTypeStorage(typeToCast, possibleTypes))
    }

    fun downCast(objectValue: ObjectValue, typeToCast: RefType): ObjectValue {
        val inheritorsTypes = findOrConstructInheritorsIncludingTypes(typeToCast)
        val possibleTypes = objectValue.possibleConcreteTypes.filter { it in inheritorsTypes }

        return wrapper(typeToCast, objectValue.addr) ?: objectValue.copy(
            typeStorage = constructTypeStorage(
                typeToCast,
                possibleTypes
            )
        )
    }

    /**
     * Connects types and number of dimensions for the two given addresses. Uses for reading from arrays:
     * cell should have the same type and number of dimensions as the objects taken/put from/in it.
     * It is a simplification, because the object can be subtype of the type of the cell, but it is ignored for now.
     */
    fun connectArrayCeilType(ceilAddr: UtAddrExpression, valueAddr: UtAddrExpression): UtBoolExpression {
        val ceilSymType = typeRegistry.symTypeId(ceilAddr)
        val valueSymType = typeRegistry.symTypeId(valueAddr)
        val ceilSymDimension = typeRegistry.symNumDimensions(ceilAddr)
        val valueSymDimension = typeRegistry.symNumDimensions(valueAddr)

        return mkAnd(mkEq(ceilSymType, valueSymType), mkEq(ceilSymDimension, valueSymDimension))
    }

    fun findAnyConcreteInheritorIncludingOrDefaultUnsafe(evaluatedType: RefType, defaultType: RefType): RefType =
        findAnyConcreteInheritorIncluding(evaluatedType) ?: findAnyConcreteInheritorIncluding(defaultType)
        ?: error("No concrete types found neither for $evaluatedType, nor for $defaultType")

    fun findAnyConcreteInheritorIncludingOrDefault(evaluatedType: RefType, defaultType: RefType): RefType? =
        findAnyConcreteInheritorIncluding(evaluatedType) ?: findAnyConcreteInheritorIncluding(defaultType)

    private fun findAnyConcreteInheritorIncluding(type: RefType): RefType? =
        if (type.sootClass.isAppropriate) {
            type
        } else {
            findOrConstructInheritorsIncludingTypes(type)
                .filterNot { !it.hasSootClass() && (it.sootClass.isOverridden || it.sootClass.isUtMock) }
                .sortedByDescending { typeRegistry.findRating(it) }
                .firstOrNull { it.sootClass.isAppropriate }
        }
}

internal const val NUMBER_OF_PREFERRED_TYPES = 3

internal val SootField.isEnumOrdinal
    get() = this.name == "ordinal" && this.declaringClass.name == ENUM_CLASSNAME

internal val ENUM_CLASSNAME: String = java.lang.Enum::class.java.canonicalName
internal val ENUM_ORDINAL = ChunkId(ENUM_CLASSNAME, "ordinal")
internal val CLASS_REF_CLASSNAME: String = Class::class.java.canonicalName
internal val CLASS_REF_CLASS_ID = Class::class.java.id

internal val CLASS_REF_TYPE_DESCRIPTOR: MemoryChunkDescriptor
    get() = MemoryChunkDescriptor(
        ChunkId(CLASS_REF_CLASSNAME, "modeledType"),
        CLASS_REF_TYPE,
        IntType.v()
    )

internal val CLASS_REF_NUM_DIMENSIONS_DESCRIPTOR: MemoryChunkDescriptor
    get() = MemoryChunkDescriptor(
        ChunkId(CLASS_REF_CLASSNAME, "modeledNumDimensions"),
        CLASS_REF_TYPE,
        IntType.v()
    )

internal val CLASS_REF_SOOT_CLASS: SootClass
    get() = Scene.v().getSootClass(CLASS_REF_CLASSNAME)

internal val OBJECT_TYPE: RefType
    get() = Scene.v().getSootClass(Object::class.java.canonicalName).type
internal val STRING_TYPE: RefType
    get() = Scene.v().getSootClass(String::class.java.canonicalName).type
internal val CLASS_REF_TYPE: RefType
    get() = CLASS_REF_SOOT_CLASS.type
internal val THREAD_TYPE: RefType
    get() = Scene.v().getSootClass(Thread::class.java.canonicalName).type
internal val THREAD_GROUP_TYPE: RefType
    get() = Scene.v().getSootClass(ThreadGroup::class.java.canonicalName).type
internal val COMPLETABLE_FUTURE_TYPE: RefType
    get() = Scene.v().getSootClass(CompletableFuture::class.java.canonicalName).type
internal val EXECUTORS_TYPE: RefType
    get() = Scene.v().getSootClass(Executors::class.java.canonicalName).type
internal val COUNT_DOWN_LATCH_TYPE: RefType
    get() = Scene.v().getSootClass(CountDownLatch::class.java.canonicalName).type

internal val NEW_INSTANCE_SIGNATURE: String = CLASS_REF_SOOT_CLASS.getMethodByName("newInstance").subSignature

internal val HASHCODE_SIGNATURE: String =
    Scene.v()
        .getSootClass(Object::class.java.canonicalName)
        .getMethodByName(Object::hashCode.name)
        .subSignature

internal val EQUALS_SIGNATURE: String =
    Scene.v()
        .getSootClass(Object::class.java.canonicalName)
        .getMethodByName(Object::equals.name)
        .subSignature

/**
 * Represents [java.lang.System.security] field signature.
 * Hardcoded string literal because it is differently processed in Android.
 */
internal const val SECURITY_FIELD_SIGNATURE: String = "<java.lang.System: java.lang.SecurityManager security>"

/**
 * Represents [sun.reflect.Reflection.fieldFilterMap] field signature.
 * Hardcoded string literal because [sun.reflect.Reflection] is removed in Java 11.
 */
internal const val FIELD_FILTER_MAP_FIELD_SIGNATURE: String = "<sun.reflect.Reflection: java.util.Map fieldFilterMap>"

/**
 * Represents [sun.reflect.Reflection.methodFilterMap] field signature.
 * Hardcoded string literal because [sun.reflect.Reflection] is removed in Java 11.
 */
internal const val METHOD_FILTER_MAP_FIELD_SIGNATURE: String = "<sun.reflect.Reflection: java.util.Map methodFilterMap>"

/**
 * Special type represents string literal, which is not String Java object
 */
object SeqType : Type() {
    override fun toString() = "SeqType"
}
