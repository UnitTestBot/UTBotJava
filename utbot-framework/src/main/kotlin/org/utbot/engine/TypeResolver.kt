package org.utbot.engine

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.mkAnd
import org.utbot.engine.pc.mkEq
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.id
import soot.ArrayType
import soot.IntType
import soot.NullType
import soot.PrimType
import soot.RefType
import soot.Scene
import soot.SootField
import soot.Type
import soot.VoidType

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
            .filterNot { (baseType, numDimensions) -> isInappropriateOrArrayOfMocksOrLocals(numDimensions, baseType) }
            .mapTo(mutableSetOf()) { (baseType, numDimensions) ->
                if (numDimensions == 0) baseType else baseType.makeArrayType(numDimensions)
            }

        return TypeStorage(type, concretePossibleTypes).removeInappropriateTypes()
    }

    private fun isInappropriateOrArrayOfMocksOrLocals(numDimensions: Int, baseType: Type?): Boolean {
        if (baseType !is RefType) {
            return false
        }

        val baseSootClass = baseType.sootClass

        if (numDimensions == 0 && baseSootClass.isInappropriate) {
            // interface, abstract class, or local, or mock could not be constructed
            return true
        }

        if (numDimensions > 0 && (baseSootClass.isLocal || baseSootClass.findMockAnnotationOrNull != null)) {
            // array of mocks or locals could not be constructed, but array of interfaces or abstract classes could be
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
        if (useConcreteType) return TypeStorage(type)

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

        return TypeStorage(type, possibleTypes).removeInappropriateTypes()
    }

    /**
     * Remove wrapper types and, if any other type is available, artificial entities.
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

            // All wrappers should filtered out because they could not be instantiated
            workaround(WorkaroundReason.HACK) {
                if (leastCommonSootClass == OBJECT_TYPE && sootClass.isOverridden) {
                    return@filter false
                }
            }

            return@filter true
        }.toSet()

        return copy(possibleConcreteTypes = appropriateTypes)
    }

    /**
     * Constructs a nullObject with TypeStorage containing all the inheritors for the given type
     */
    fun nullObject(type: Type) = when (type) {
        is RefType, is NullType, is VoidType -> ObjectValue(TypeStorage(type), nullObjectAddr)
        is ArrayType -> ArrayValue(TypeStorage(type), nullObjectAddr)
        else -> error("Unsupported nullType $type")
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

internal val OBJECT_TYPE: RefType
    get() = Scene.v().getSootClass(Object::class.java.canonicalName).type
internal val STRING_TYPE: RefType
    get() = Scene.v().getSootClass(String::class.java.canonicalName).type
internal val CLASS_REF_TYPE: RefType
    get() = Scene.v().getSootClass(CLASS_REF_CLASSNAME).type

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
