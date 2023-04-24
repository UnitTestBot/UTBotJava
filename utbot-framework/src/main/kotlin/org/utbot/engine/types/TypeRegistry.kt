package org.utbot.engine.types

import com.google.common.collect.HashBiMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.utbot.common.WorkaroundReason
import org.utbot.common.doNotRun
import org.utbot.common.workaround
import org.utbot.engine.ChunkId
import org.utbot.engine.MAX_NUM_DIMENSIONS
import org.utbot.engine.MemoryUpdate
import org.utbot.engine.MethodResult
import org.utbot.engine.ObjectValue
import org.utbot.engine.TypeConstraint
import org.utbot.engine.TypeStorage
import org.utbot.engine.baseType
import org.utbot.engine.classBytecodeSignatureToClassNameOrNull
import org.utbot.engine.findMockAnnotationOrNull
import org.utbot.engine.getByValue
import org.utbot.engine.isAnonymous
import org.utbot.engine.isInappropriate
import org.utbot.engine.isJavaLangObject
import org.utbot.engine.nullObjectAddr
import org.utbot.engine.numDimensions
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtAddrSort
import org.utbot.engine.pc.UtArrayExpressionBase
import org.utbot.engine.pc.UtArraySort
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.UtBoolSort
import org.utbot.engine.pc.UtEqGenericTypeParametersExpression
import org.utbot.engine.pc.UtFalse
import org.utbot.engine.pc.UtGenericExpression
import org.utbot.engine.pc.UtInt32Sort
import org.utbot.engine.pc.UtIsExpression
import org.utbot.engine.pc.UtIsGenericTypeExpression
import org.utbot.engine.pc.UtMkTermArrayExpression
import org.utbot.engine.pc.UtTrue
import org.utbot.engine.pc.mkAnd
import org.utbot.engine.pc.mkArrayConst
import org.utbot.engine.pc.mkArrayWithConst
import org.utbot.engine.pc.mkEq
import org.utbot.engine.pc.mkInt
import org.utbot.engine.pc.mkNot
import org.utbot.engine.pc.mkOr
import org.utbot.engine.pc.select
import org.utbot.engine.pc.store
import org.utbot.engine.namedStore
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.toIntValue
import org.utbot.engine.toPrimitiveValue
import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.RefType
import soot.Scene
import soot.ShortType
import soot.SootClass
import soot.SootField
import soot.SootMethod
import soot.Type
import soot.tagkit.AnnotationClassElem
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Types/classes registry.
 *
 * Registers and keeps two mappings:
 * - Type <-> unique type id (int)
 * - Object address -> to type id
 */
class TypeRegistry {
    init {
        // initializes type storage for OBJECT_TYPE from current scene
        objectTypeStorage = TypeStorage.constructTypeStorageUnsafe(
            OBJECT_TYPE,
            Scene.v().classes.mapTo(mutableSetOf()) { it.type }
        )
    }

    private val typeIdBiMap = HashBiMap.create<Type, Int>()

    // A cache for strings representing bit-vectors for some collection of types.
    private val typesToBitVecString = mutableMapOf<Collection<Type>, String>()
    private val typeToRating = mutableMapOf<RefType, Int>()
    private val typeToInheritorsTypes = mutableMapOf<RefType, Set<RefType>>()
    private val typeToAncestorsTypes = mutableMapOf<RefType, Set<RefType>>()

    // A BiMap containing bijection from every type to an address of the object
    // presenting its classRef and vise versa
    private val classRefBiMap = HashBiMap.create<Type, UtAddrExpression>()

    /**
     * Contains mapping from a class to the class containing substitutions for its methods.
     */
    private val targetToSubstitution: Map<SootClass, SootClass> by lazy {
        val classesWithTargets = Scene.v().classes.mapNotNull { clazz ->
            val annotation = clazz.findMockAnnotationOrNull
                ?.elems
                ?.singleOrNull { it.name == "target" } as? AnnotationClassElem

            val classNameFromSignature = classBytecodeSignatureToClassNameOrNull(annotation?.desc)

            if (classNameFromSignature == null) {
                null
            } else {
                val target = Scene.v().getSootClass(classNameFromSignature)
                target to clazz
            }
        }
        classesWithTargets.toMap()
    }

    /**
     * Contains mapping from a class with substitutions of the methods of the target class to the target class itself.
     */
    private val substitutionToTarget: Map<SootClass, SootClass> by lazy {
        targetToSubstitution.entries.associate { (k, v) -> v to k }
    }

    private val typeToFields = mutableMapOf<RefType, List<SootField>>()

    /**
     * An array containing information about whether the object with particular addr could throw a [ClassCastException].
     *
     * Note: all objects can throw it by default.
     * @see disableCastClassExceptionCheck
     */
    private var isClassCastExceptionAllowed: UtArrayExpressionBase =
        mkArrayWithConst(UtArraySort(UtAddrSort, UtBoolSort), UtTrue)


    /**
     * Contains information about types for ReferenceValues.
     * An element on some position k contains information about type for an object with address == k
     * Each element in addrToTypeId is in range [1..numberOfTypes]
     */
    private val addrToTypeId: UtArrayExpressionBase by lazy {
        mkArrayConst(
            "addrToTypeId",
            UtAddrSort,
            UtInt32Sort
        )
    }

    private val genericAddrToTypeArrays = mutableMapOf<Int, UtArrayExpressionBase>()

    private fun genericAddrToType(i: Int) = genericAddrToTypeArrays.getOrPut(i) {
        mkArrayConst(
            "genericAddrToTypeId_$i",
            UtAddrSort,
            UtInt32Sort
        )
    }

    /**
     * Contains information about number of dimensions for ReferenceValues.
     */
    private val addrToNumDimensions: UtArrayExpressionBase by lazy {
        mkArrayConst(
            "addrToNumDimensions",
            UtAddrSort,
            UtInt32Sort
        )
    }

    private val genericAddrToNumDimensionsArrays = mutableMapOf<Int, UtArrayExpressionBase>()

    private fun genericAddrToNumDimensions(i: Int) = genericAddrToNumDimensionsArrays.getOrPut(i) {
        mkArrayConst(
            "genericAddrToNumDimensions_$i",
            UtAddrSort,
            UtInt32Sort
        )
    }

    /**
     * Contains information about whether the object with some addr is a mock or not.
     */
    private val isMockArray: UtArrayExpressionBase by lazy {
        mkArrayConst(
            "isMock",
            UtAddrSort,
            UtBoolSort
        )
    }

    /**
     * Takes information about whether the object with [addr] is mock or not.
     *
     * @see isMockArray
     */
    fun isMock(addr: UtAddrExpression) = isMockArray.select(addr)

    private fun mockCorrectnessConstraint(addr: UtAddrExpression) =
        mkOr(
            mkEq(isMock(addr), UtFalse),
            mkNot(mkEq(addr, nullObjectAddr))
        )

    fun isMockConstraint(addr: UtAddrExpression) = mkAnd(
        mkOr(
            mkEq(isMock(addr), UtTrue),
            mkEq(addr, nullObjectAddr)
        ),
        mockCorrectnessConstraint(addr)
    )

    /**
     * Makes the numbers of dimensions for every object in the program equal to zero by default
     */
    fun softZeroNumDimensions() = UtMkTermArrayExpression(addrToNumDimensions)

    /**
     * addrToTypeId is created as const array of the emptyType. If such type occurs anywhere in the program, it means
     * we haven't touched the element that this type belongs to
     * @see emptyTypeId
     */
    fun softEmptyTypes() = UtMkTermArrayExpression(addrToTypeId, mkInt(emptyTypeId))

    /**
     * Calculates type 'rating' for a particular type. Used for ordering ObjectValue's possible types.
     * The type with a higher rating is more likely than the one with a lower rating.
     */
    fun findRating(type: RefType) = typeToRating.getOrPut(type) {
        var finalCost = 0

        val sootClass = type.sootClass

        // TODO: let's have "preferred types"
        if (sootClass.name == "java.util.ArrayList") finalCost += 4096
        if (sootClass.name == "java.util.LinkedList") finalCost += 2048
        if (sootClass.name == "java.util.HashMap") finalCost += 4096
        if (sootClass.name == "java.util.TreeMap") finalCost += 2048
        if (sootClass.name == "java.util.HashSet") finalCost += 2048
        if (sootClass.name == "java.lang.Integer") finalCost += 8192
        if (sootClass.name == "java.lang.Character") finalCost += 8192
        if (sootClass.name == "java.lang.Double") finalCost += 8192
        if (sootClass.name == "java.lang.Long") finalCost += 8192

        if (sootClass.packageName.startsWith("java.lang")) finalCost += 1024

        if (sootClass.packageName.startsWith("java.util")) finalCost += 512

        if (sootClass.packageName.startsWith("java")) finalCost += 128

        if (sootClass.isPublic) finalCost += 16

        if (sootClass.isPrivate) finalCost += -16

        if ("blocking" in sootClass.name.toLowerCase()) finalCost -= 32

        if (sootClass.type.isJavaLangObject()) finalCost += -32

        if (sootClass.isAnonymous) finalCost += -128

        if (sootClass.name.contains("$")) finalCost += -4096

        if (sootClass.type.sootClass.isInappropriate) finalCost += -8192

        finalCost
    }

    private val classRefCounter = AtomicInteger(classRefAddrsInitialValue)
    private fun nextClassRefAddr() = UtAddrExpression(classRefCounter.getAndIncrement())

    private val symbolicReturnNameCounter = AtomicLong(symbolicReturnNameCounterInitialValue)
    fun findNewSymbolicReturnValueName() =
        workaround(WorkaroundReason.MAKE_SYMBOLIC) { "symbolicReturnValue\$${symbolicReturnNameCounter.incrementAndGet()}" }

    private val typeCounter = AtomicInteger(typeCounterInitialValue)
    private fun nextTypeId() = typeCounter.getAndIncrement()

    /**
     * Returns unique typeId for the given type
     */
    fun findTypeId(type: Type): Int = typeIdBiMap.getOrPut(type) { nextTypeId() }

    /**
     * Returns type for the given typeId
     *
     * @return If there is such typeId in the program, returns the corresponding type, otherwise returns null
     */
    fun typeByIdOrNull(typeId: Int): Type? = typeIdBiMap.getByValue(typeId)

    /**
     * Returns symbolic representation for a typeId corresponding to the given address
     */
    fun symTypeId(addr: UtAddrExpression) = addrToTypeId.select(addr)

    /**
     * Returns a symbolic representation for an [i]th type parameter
     * corresponding to the given address
     */
    fun genericTypeId(addr: UtAddrExpression, i: Int) = genericAddrToType(i).select(addr)

    /**
     * Returns symbolic representation for a number of dimensions corresponding to the given address
     */
    fun symNumDimensions(addr: UtAddrExpression) = addrToNumDimensions.select(addr)

    fun genericNumDimensions(addr: UtAddrExpression, i: Int) = genericAddrToNumDimensions(i).select(addr)

    /**
     * Returns a constraint stating that number of dimensions for the given address is zero
     */
    fun zeroDimensionConstraint(addr: UtAddrExpression) = mkEq(symNumDimensions(addr), mkInt(objectNumDimensions))

    /**
     * Constructs a binary bit-vector by the given types with length 'numberOfTypes'. Each position
     * corresponding to one of the typeId.
     *
     * @param types  the collection of possible type
     * @return decimal string representing the binary bit-vector
     */
    fun constructBitVecString(types: Collection<Type>) = typesToBitVecString.getOrPut(types) {
        val initialValue = BigInteger(ByteArray(numberOfTypes) { 0 })

        return types.fold(initialValue) { acc, type ->
            val typeId = if (type is ArrayType) findTypeId(type.baseType) else findTypeId(type)
            acc.setBit(typeId)
        }.toString()
    }

    /**
     * Creates class reference, i.e. Class&lt;Integer&gt;
     *
     * Note: Uses type id as an address to have the one and the same class reference for all objects of one class
     */
    fun createClassRef(baseType: Type, numDimensions: Int = 0): MethodResult {
        val addr = classRefBiMap.getOrPut(baseType) { nextClassRefAddr() }

        val typeStorage = TypeStorage.constructTypeStorageWithSingleType(CLASS_REF_TYPE)
        val objectValue = ObjectValue(typeStorage, addr)

        val typeConstraint = typeConstraint(addr, typeStorage).all()

        val typeId = mkInt(findTypeId(baseType))
        val symNumDimensions = mkInt(numDimensions)

        val stores = persistentListOf(
            namedStore(CLASS_REF_TYPE_DESCRIPTOR, addr, typeId),
            namedStore(CLASS_REF_NUM_DIMENSIONS_DESCRIPTOR, addr, symNumDimensions)
        )

        val touchedDescriptors = persistentSetOf(CLASS_REF_TYPE_DESCRIPTOR, CLASS_REF_NUM_DIMENSIONS_DESCRIPTOR)

        val memoryUpdate = MemoryUpdate(
            stores = stores,
            touchedChunkDescriptors = touchedDescriptors,
        )

        return MethodResult(objectValue, typeConstraint.asHardConstraint(), memoryUpdates = memoryUpdate)
    }

    /**
     * Returns a list of inheritors for the given [type], including itself.
     */
    fun findInheritorsIncludingTypes(type: RefType, defaultValue: () -> Set<RefType>) =
        typeToInheritorsTypes.getOrPut(type, defaultValue)

    /**
     * Returns a list of ancestors for the given [type], including itself.
     */
    fun findAncestorsIncludingTypes(type: RefType, defaultValue: () -> Set<RefType>) =
        typeToAncestorsTypes.getOrPut(type, defaultValue)

    fun findFields(type: RefType, defaultValue: () -> List<SootField>) =
        typeToFields.getOrPut(type, defaultValue)

    /**
     * Returns a [TypeConstraint] instance for the given [addr] and [typeStorage].
     */
    fun typeConstraint(addr: UtAddrExpression, typeStorage: TypeStorage): TypeConstraint =
        TypeConstraint(
            constructIsExpression(addr, typeStorage),
            mkEq(addr, nullObjectAddr),
            constructCorrectnessConstraint(addr, typeStorage)
        )

    private fun constructIsExpression(addr: UtAddrExpression, typeStorage: TypeStorage): UtIsExpression =
        UtIsExpression(addr, typeStorage, numberOfTypes)

    /**
     * Returns a conjunction of the constraints responsible for the type construction:
     * * typeId must be in range [[emptyTypeId]..[numberOfTypes]];
     * * numDimensions must be in range [0..[MAX_NUM_DIMENSIONS]];
     * * if the baseType for [TypeStorage.leastCommonType] is a [java.lang.Object],
     * should be added constraints for primitive arrays to prevent
     * impossible resolved types: Object[] must be at least primType[][].
     */
    private fun constructCorrectnessConstraint(addr: UtAddrExpression, typeStorage: TypeStorage): UtBoolExpression {
        val symType = symTypeId(addr)
        val symNumDimensions = symNumDimensions(addr)
        val type = typeStorage.leastCommonType

        val constraints = mutableListOf<UtBoolExpression>()

        // add constraints for typeId, it must be in 0..numberOfTypes
        constraints += org.utbot.engine.Ge(symType.toIntValue(), emptyTypeId.toPrimitiveValue())
        constraints += org.utbot.engine.Le(symType.toIntValue(), numberOfTypes.toPrimitiveValue())

        // add constraints for number of dimensions, it must be in 0..MAX_NUM_DIMENSIONS
        constraints += org.utbot.engine.Ge(symNumDimensions.toIntValue(), 0.toPrimitiveValue())
        constraints += org.utbot.engine.Le(symNumDimensions.toIntValue(), MAX_NUM_DIMENSIONS.toPrimitiveValue())

        doNotRun {
            // add constraints for object and arrays of primitives
            if (type.baseType.isJavaLangObject()) {
                primTypes.forEach {
                    val typesAreEqual = mkEq(symType, mkInt(findTypeId(it)))
                    val numDimensions =
                        org.utbot.engine.Gt(symNumDimensions.toIntValue(), type.numDimensions.toPrimitiveValue())
                    constraints += mkOr(mkNot(typesAreEqual), numDimensions)
                }
            }

            // there are no arrays of anonymous classes
            typeStorage.possibleConcreteTypes
                .mapNotNull { (it.baseType as? RefType) }
                .filter { it.sootClass.isAnonymous }
                .forEach {
                    val typesAreEqual = mkEq(symType, mkInt(findTypeId(it)))
                    val numDimensions = mkEq(symNumDimensions.toIntValue(), mkInt(objectNumDimensions).toIntValue())
                    constraints += mkOr(mkNot(typesAreEqual), numDimensions)
                }
        }

        return mkAnd(constraints)
    }

    /**
     * returns constraint representing, that object with address [addr] is parametrized by [types] type parameters.
     * @see UtGenericExpression
     */
    fun genericTypeParameterConstraint(addr: UtAddrExpression, types: List<TypeStorage>) =
        UtGenericExpression(addr, types, numberOfTypes)

    /**
     * Returns constraint representing that an object with address [addr] has the same type as the type parameter
     * with index [i] of an object with address [baseAddr].
     *
     * For a SomeCollection<A, B> the type parameters are [A, B], where A and B are type variables
     * with indices zero and one respectively. To connect some element of the collection with its generic type
     * add to the constraints `typeConstraintToGenericTypeParameter(elementAddr, collectionAddr, typeParamIndex)`.
     *
     * @see UtIsGenericTypeExpression
     */
    fun typeConstraintToGenericTypeParameter(
        addr: UtAddrExpression,
        baseAddr: UtAddrExpression,
        i: Int
    ): UtIsGenericTypeExpression = UtIsGenericTypeExpression(addr, baseAddr, i)

    /**
     * Looks for a substitution for the given [method].
     *
     * @param method a method to be substituted.
     * @return substituted method if the given [method] has substitution, null otherwise.
     *
     * Note: all the methods in the class with substitutions will be returned instead of methods of the target class
     * with the same name and parameters' types without any additional annotations. The only exception is `<init>`
     * method, substitutions will be returned only for constructors marked by [org.utbot.api.annotation.UtConstructorMock]
     * annotation.
     */
    fun findSubstitutionOrNull(method: SootMethod): SootMethod? {
        val declaringClass = method.declaringClass
        val classWithSubstitutions = targetToSubstitution[declaringClass]

        val substitutedMethod = classWithSubstitutions
            ?.methods
            ?.singleOrNull { it.name == method.name && it.parameterTypes == method.parameterTypes }
        // Note: subSignature is not used in order to support `this` as method's return value.
        // Otherwise we'd have to check for wrong `this` type in the subSignature

        if (method.isConstructor) {
            // if the constructor doesn't have the mock annotation do not substitute it
            substitutedMethod?.findMockAnnotationOrNull ?: return null
        }
        return substitutedMethod
    }

    /**
     * Returns a class containing substitutions for the methods belong to the target class, null if there is not such class.
     */
    @Suppress("unused")
    fun findSubstitutionByTargetOrNull(targetClass: SootClass): SootClass? = targetToSubstitution[targetClass]

    /**
     * Returns a target class by given class with methods substitutions.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun findTargetBySubstitutionOrNull(classWithSubstitutions: SootClass): SootClass? =
        substitutionToTarget[classWithSubstitutions]

    /**
     * Looks for 'real' type.
     *
     * For example, we have two classes: A and B, B contains substitutions for A's methods.
     * `findRealType(a.type)` will return `a.type`, but `findRealType(b.type)` will return `a.type` as well.
     *
     * Returns:
     * * [type] if it is not a RefType;
     * * [type] if it is a RefType and it doesn't have a target class to substitute;
     * * otherwise a type of the target class, which methods should be substituted.
     */
    fun findRealType(type: Type): Type =
        if (type !is RefType) type else findTargetBySubstitutionOrNull(type.sootClass)?.type ?: type

    /**
     * Returns a select expression containing information about whether [ClassCastException] is allowed or not
     * for an object with the given [addr].
     *
     * True means that [ClassCastException] might be thrown, false will restrict it.
     */
    fun isClassCastExceptionAllowed(addr: UtAddrExpression) = isClassCastExceptionAllowed.select(addr)

    /**
     * Modify [isClassCastExceptionAllowed] to make impossible for a [ClassCastException] to be thrown for an object
     * with the given [addr].
     */
    fun disableCastClassExceptionCheck(addr: UtAddrExpression) {
        isClassCastExceptionAllowed = isClassCastExceptionAllowed.store(addr, UtFalse)
    }

    /**
     * Returns chunkId for the given [arrayType].
     *
     * Examples:
     * * Object[] -> RefValues_Arrays
     * * int[] -> intArrays
     * * int[][] -> MultiArrays
     */
    fun arrayChunkId(arrayType: ArrayType) = when (arrayType.numDimensions) {
        1 -> if (arrayType.baseType is RefType) {
            ChunkId("RefValues", "Arrays")
        } else {
            ChunkId("${findRealType(arrayType.baseType)}", "Arrays")
        }
        else -> ChunkId("Multi", "Arrays")
    }

    companion object {
        // we use different shifts to distinguish easily types from objects in z3 listings
        const val objectCounterInitialValue = 0x00000001 // 0x00000000 is reserved for NULL

        // we want to reserve addresses for every ClassRef in the program starting from this one
        // Note: the number had been chosen randomly and can be changes without any consequences
        const val classRefAddrsInitialValue = -16777216 // -(2 ^ 24)

        // since we use typeId as addr for ConstRef, we can not use 0x00000000 because of NULL value
        const val typeCounterInitialValue = 0x00000001
        const val symbolicReturnNameCounterInitialValue = 0x80000000
        const val objectNumDimensions = 0
        const val emptyTypeId = 0
        private const val primitivesNumber = 8

        internal val primTypes
            get() = listOf(
                ByteType.v(),
                ShortType.v(),
                IntType.v(),
                LongType.v(),
                FloatType.v(),
                DoubleType.v(),
                BooleanType.v(),
                CharType.v()
            )

        val numberOfTypes get() = Scene.v().classes.size + primitivesNumber + typeCounterInitialValue

        /**
         * Stores [TypeStorage] for [OBJECT_TYPE]. As it should be changed when Soot scene changes,
         * it is loaded each time when [TypeRegistry] is created in init section.
         */
        lateinit var objectTypeStorage: TypeStorage
    }
}