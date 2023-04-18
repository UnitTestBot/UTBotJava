package org.utbot.engine

import org.utbot.engine.overrides.collections.AssociativeArray
import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtAddrSort
import org.utbot.engine.pc.UtArrayInsert
import org.utbot.engine.pc.UtArrayInsertRange
import org.utbot.engine.pc.UtArrayRemove
import org.utbot.engine.pc.UtArrayRemoveRange
import org.utbot.engine.pc.UtArraySetRange
import org.utbot.engine.pc.UtArrayShiftIndexes
import org.utbot.engine.pc.UtArraySort
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.UtIntSort
import org.utbot.engine.pc.mkArrayWithConst
import org.utbot.engine.pc.mkInt
import org.utbot.engine.pc.select
import org.utbot.engine.pc.store
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.types.OBJECT_TYPE
import org.utbot.engine.types.TypeRegistry
import org.utbot.engine.types.TypeResolver
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.getIdOrThrow
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import soot.ArrayType
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.SootField
import soot.SootMethod

val rangeModifiableArrayId: ClassId = RangeModifiableUnlimitedArray::class.id

class RangeModifiableUnlimitedArrayWrapper : WrapperInterface {
    @Suppress("UNUSED_PARAMETER")
    private fun initMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val arrayAddr = findNewAddr()

            listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    memoryUpdates = arrayUpdateWithValue(
                        arrayAddr,
                        OBJECT_TYPE.arrayType,
                        mkArrayWithConst(UtArraySort(UtIntSort, UtAddrSort), mkInt(0))
                    )
                            + objectUpdate(wrapper, storageField, arrayAddr)
                            + objectUpdate(wrapper, beginField, mkInt(0))
                            + objectUpdate(wrapper, endField, mkInt(0))
                )
            )
        }

    @Suppress("UNUSED_PARAMETER")
    private fun insertMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val value = UtArrayInsert(
                getStorageArrayExpression(wrapper),
                parameters[0] as PrimitiveValue,
                parameters[1].addr
            )

            listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    memoryUpdates = arrayUpdateWithValue(
                        getStorageArrayField(wrapper.addr).addr,
                        OBJECT_TYPE.arrayType,
                        value
                    )
                )
            )
        }

    @Suppress("UNUSED_PARAMETER")
    private fun insertRangeMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val value = UtArrayInsertRange(
                getStorageArrayExpression(wrapper),
                parameters[0] as PrimitiveValue,
                selectArrayExpressionFromMemory(parameters[1] as ArrayValue),
                parameters[2] as PrimitiveValue,
                parameters[3] as PrimitiveValue
            )
            listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    memoryUpdates = arrayUpdateWithValue(
                        getStorageArrayField(wrapper.addr).addr,
                        OBJECT_TYPE.arrayType,
                        value
                    ),
                )
            )
        }

    @Suppress("UNUSED_PARAMETER")
    private fun removeMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val value = UtArrayRemove(
                getStorageArrayExpression(wrapper),
                parameters[0] as PrimitiveValue
            )
            listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    memoryUpdates = arrayUpdateWithValue(
                        getStorageArrayField(wrapper.addr).addr,
                        OBJECT_TYPE.arrayType,
                        value
                    ),
                )
            )
        }

    @Suppress("UNUSED_PARAMETER")
    private fun removeRangeMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val value = UtArrayRemoveRange(
                getStorageArrayExpression(wrapper),
                parameters[0] as PrimitiveValue,
                parameters[1] as PrimitiveValue
            )
            listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    memoryUpdates = arrayUpdateWithValue(
                        getStorageArrayField(wrapper.addr).addr,
                        OBJECT_TYPE.arrayType,
                        value
                    ),
                )
            )
        }

    @Suppress("UNUSED_PARAMETER")
    private fun setMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val value =
                getStorageArrayExpression(wrapper).store((parameters[0] as PrimitiveValue).expr, parameters[1].addr)
            listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    memoryUpdates = arrayUpdateWithValue(
                        getStorageArrayField(wrapper.addr).addr,
                        OBJECT_TYPE.arrayType,
                        value
                    ),
                )
            )
        }

    @Suppress("UNUSED_PARAMETER")
    private fun getMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val value = getStorageArrayExpression(wrapper).select((parameters[0] as PrimitiveValue).expr)
            val addr = UtAddrExpression(value)

            // Try to retrieve manually set type if present
            val valueType = extractSingleTypeParameterForRangeModifiableArray(wrapper.addr, memory)
                ?.leastCommonType
                ?: OBJECT_TYPE

            val resultObject = if (valueType is RefType) {
                val mockInfoGenerator = UtMockInfoGenerator { mockAddr ->
                    UtObjectMockInfo(valueType.id, mockAddr)
                }
                createObject(addr, valueType, useConcreteType = false, mockInfoGenerator)
            } else {
                require(valueType is ArrayType) {
                    "Unexpected Primitive Type $valueType in generic parameter for RangeModifiableUnlimitedArray $wrapper"
                }

                createArray(addr, valueType, useConcreteType = false)
            }

            val typeIndex = wrapper.asWrapperOrNull?.getOperationTypeIndex
                ?: error("Wrapper was expected, got $wrapper")
            val typeConstraint = typeRegistry.typeConstraintToGenericTypeParameter(
                addr,
                wrapper.addr,
                i = typeIndex
            ).asHardConstraint()

            val methodResult = MethodResult(SymbolicSuccess(resultObject), typeConstraint)

            listOf(methodResult)
        }

    @Suppress("UNUSED_PARAMETER")
    private fun toArrayMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val arrayAddr = findNewAddr()
            val offset = parameters[0] as PrimitiveValue
            val length = parameters[1] as PrimitiveValue

            val value = UtArrayShiftIndexes(getStorageArrayExpression(wrapper), offset)

            val typeStorage = typeResolver.constructTypeStorage(OBJECT_TYPE.arrayType, useConcreteType = false)
            val array = ArrayValue(typeStorage, arrayAddr)

            val hardConstraints = setOf(
                Eq(memory.findArrayLength(arrayAddr), length),
                typeRegistry.typeConstraint(arrayAddr, array.typeStorage).all(),
            ).asHardConstraint()

            listOf(
                MethodResult(
                    SymbolicSuccess(array),
                    hardConstraints = hardConstraints,
                    memoryUpdates = arrayUpdateWithValue(arrayAddr, OBJECT_TYPE.arrayType, value)
                )
            )
        }

    @Suppress("UNUSED_PARAMETER")
    private fun setRangeMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val value = UtArraySetRange(
                getStorageArrayExpression(wrapper),
                parameters[0] as PrimitiveValue,
                selectArrayExpressionFromMemory(parameters[1] as ArrayValue),
                parameters[2] as PrimitiveValue,
                parameters[3] as PrimitiveValue
            )
            listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    memoryUpdates = arrayUpdateWithValue(
                        getStorageArrayField(wrapper.addr).addr,
                        OBJECT_TYPE.arrayType,
                        value
                    ),
                )
            )
        }

    override val wrappedMethods: Map<String, MethodSymbolicImplementation> =
        mapOf(
            "<init>" to ::initMethodWrapper,
            "insert" to ::insertMethodWrapper,
            "insertRange" to ::insertRangeMethodWrapper,
            "remove" to ::removeMethodWrapper,
            "removeRange" to ::removeRangeMethodWrapper,
            "set" to ::setMethodWrapper,
            "get" to ::getMethodWrapper,
            "toArray" to ::toArrayMethodWrapper,
            "setRange" to ::setRangeMethodWrapper,
        )

    private fun Traverser.getStorageArrayField(addr: UtAddrExpression) =
        getArrayField(addr, rangeModifiableArrayClass, storageField)

    private fun Traverser.getStorageArrayExpression(
        wrapper: ObjectValue
    ): UtExpression = selectArrayExpressionFromMemory(getStorageArrayField(wrapper.addr))

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel {
        // get addr of array in the storage field
        val storageFieldChunkId = resolver.hierarchy.chunkIdForField(wrapper.type, storageField)
        val storageFieldArrayDescriptor = MemoryChunkDescriptor(storageFieldChunkId, wrapper.type, OBJECT_TYPE.arrayElementType)
        val arrayAddr = resolver.findArray(storageFieldArrayDescriptor, resolver.state).select(wrapper.addr)

        // get arrayExpression from arrayChunk with object type by arrayAddr
        val arrayValuesChunkId = resolver.typeRegistry.arrayChunkId(OBJECT_TYPE.arrayType)
        val arrayValuesDescriptor = MemoryChunkDescriptor(arrayValuesChunkId, OBJECT_TYPE.arrayType, OBJECT_TYPE)
        val arrayExpression = resolver.findArray(arrayValuesDescriptor, resolver.state).select(arrayAddr)

        // resolve borders of array
        val firstValue = resolver.resolveIntField(wrapper, beginField)
        val lastValue = resolver.resolveIntField(wrapper, endField)

        // check that size of array >= 0 and is not too big for resolver
        val sizeValue = if (lastValue - firstValue in 0..MAX_RESOLVE_LIST_SIZE) lastValue - firstValue else 0

        // construct model values of an array

        val concreteAddr = resolver.holder.concreteAddr(wrapper.addr)

        val resultModel = UtArrayModel(
            concreteAddr,
            objectArrayClassId,
            sizeValue,
            UtNullModel(objectClassId),
            mutableMapOf()
        )

        // Collection might contain itself as an element, so we must add
        // the constructed model to avoid infinite recursion below
        resolver.addConstructedModel(concreteAddr, resultModel)

        // try to retrieve type storage for the single type parameter
        val typeStorage = extractSingleTypeParameterForRangeModifiableArray(wrapper.addr, resolver.memory)
            ?: TypeRegistry.objectTypeStorage

        (0 until sizeValue).associateWithTo(resultModel.stores) { i ->
            val addr = UtAddrExpression(arrayExpression.select(mkInt(i + firstValue)))

            val value = if (typeStorage.leastCommonType is ArrayType) {
                ArrayValue(typeStorage, addr)
            } else {
                ObjectValue(typeStorage, addr)
            }

            resolver.resolveModel(value)
        }

        return resultModel
    }

    private fun extractSingleTypeParameterForRangeModifiableArray(
        addr: UtAddrExpression,
        memory: Memory
    ): TypeStorage? = TypeResolver.extractTypeStorageForObjectWithSingleTypeParameter(
        addr,
        objectClassName = "Range modifiable array",
        memory
    )

    companion object {
        internal val rangeModifiableArrayClass: SootClass
            get() = Scene.v().getSootClass(rangeModifiableArrayId.name)
        internal val beginField: SootField
            get() = rangeModifiableArrayClass.getFieldByName("begin")
        internal val endField: SootField
            get() = rangeModifiableArrayClass.getFieldByName("end")
        internal val storageField: SootField
            get() = rangeModifiableArrayClass.getFieldByName("storage")
    }
}

val associativeArrayId: ClassId = AssociativeArray::class.id

class AssociativeArrayWrapper : WrapperInterface {

    private val associativeArrayClass = Scene.v().classes.single { associativeArrayId.name == it.name }
    private val sizeField = associativeArrayClass.getField("int size")
    private val touchedField = associativeArrayClass.getField("java.lang.Object[] touched")
    private val storageField = associativeArrayClass.getField("java.lang.Object[] storage")

    @Suppress("UNUSED_PARAMETER")
    private fun initMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val storageArrayAddr = findNewAddr()
            val touchedArrayAddr = findNewAddr()

            return listOf(
                MethodResult(
                    SymbolicSuccess(voidValue),
                    memoryUpdates = arrayUpdateWithValue(
                        storageArrayAddr,
                        OBJECT_TYPE.arrayType,
                        mkArrayWithConst(UtArraySort(UtAddrSort, UtAddrSort), mkInt(0))
                    ) + arrayUpdateWithValue(
                        touchedArrayAddr,
                        OBJECT_TYPE.arrayType,
                        mkArrayWithConst(UtArraySort(UtIntSort, UtAddrSort), mkInt(0))
                    )
                            + objectUpdate(wrapper, storageField, storageArrayAddr)
                            + objectUpdate(wrapper, touchedField, touchedArrayAddr)
                            + objectUpdate(wrapper, sizeField, mkInt(0))
                )
            )
        }

    @Suppress("UNUSED_PARAMETER")
    private fun selectMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val value = getStorageArrayExpression(wrapper).select(parameters[0].addr)
            val addr = UtAddrExpression(value)
            // TODO it is probably a bug, what mock generator should we provide here?
            //      Seems like we don't know anything about its type here
            val resultObject = createObject(addr, OBJECT_TYPE, useConcreteType = false, mockInfoGenerator = null)

            val typeIndex = wrapper.asWrapperOrNull?.selectOperationTypeIndex
                ?: error("Wrapper was expected, got $wrapper")
            val hardConstraints = typeRegistry.typeConstraintToGenericTypeParameter(
                addr,
                wrapper.addr,
                typeIndex
            ).asHardConstraint()

            val methodResult = MethodResult(SymbolicSuccess(resultObject), hardConstraints)

            listOf(methodResult)
        }

    @Suppress("UNUSED_PARAMETER")
    private fun storeMethodWrapper(
        traverser: Traverser,
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<MethodResult> =
        with(traverser) {
            val storageValue = getStorageArrayExpression(wrapper).store(parameters[0].addr, parameters[1].addr)
            val sizeValue = getIntFieldValue(wrapper, sizeField)

            // it is the reason why it's important to use an `oldKey` in `UtHashMap.put` method.
            // We navigate in the associative array using only this old address, not a new one.
            val touchedValue = getTouchedArrayExpression(wrapper).store(sizeValue, parameters[0].addr)
            val storageArrayAddr = getStorageArrayField(wrapper.addr).addr
            val touchedArrayFieldAddr = getTouchedArrayField(wrapper.addr).addr

            val storageArrayUpdate = arrayUpdateWithValue(
                storageArrayAddr,
                OBJECT_TYPE.arrayType,
                storageValue
            )

            val touchedArrayUpdate = arrayUpdateWithValue(
                touchedArrayFieldAddr,
                OBJECT_TYPE.arrayType,
                touchedValue,
            )

            val sizeUpdate = objectUpdate(wrapper, sizeField, Add(sizeValue.toIntValue(), 1.toPrimitiveValue()))

            val memoryUpdates = storageArrayUpdate + touchedArrayUpdate + sizeUpdate
            val methodResult = MethodResult(SymbolicSuccess(voidValue), memoryUpdates = memoryUpdates)

            listOf(methodResult)
        }

    override val wrappedMethods: Map<String, MethodSymbolicImplementation> = mapOf(
        "<init>" to ::initMethodWrapper,
        "select" to ::selectMethodWrapper,
        "store" to ::storeMethodWrapper,
    )

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel {
        // get arrayExpression from arrayChunk with object type by arrayAddr
        val arrayValuesChunkId = resolver.typeRegistry.arrayChunkId(OBJECT_TYPE.arrayType)

        val touchedFieldChunkId = resolver.hierarchy.chunkIdForField(wrapper.type, touchedField)
        val touchedFieldDescriptor = MemoryChunkDescriptor(touchedFieldChunkId, wrapper.type, OBJECT_TYPE.arrayElementType)
        val touchedArrayAddr = resolver.findArray(touchedFieldDescriptor, MemoryState.CURRENT).select(wrapper.addr)

        val arrayValuesDescriptor = MemoryChunkDescriptor(arrayValuesChunkId, OBJECT_TYPE.arrayType, OBJECT_TYPE)
        val touchedArrayExpression = resolver.findArray(arrayValuesDescriptor, resolver.state).select(touchedArrayAddr)

        // resolve borders of array
        val sizeValue = resolver.resolveIntField(wrapper, sizeField)

        // construct model values of an array
        val touchedValues = UtArrayModel(
            resolver.holder.concreteAddr(UtAddrExpression(touchedArrayAddr)),
            objectArrayClassId,
            sizeValue,
            UtNullModel(objectClassId),
            stores = (0 until sizeValue).associateWithTo(mutableMapOf()) { i ->
                resolver.resolveModel(
                    ObjectValue(
                        TypeStorage.constructTypeStorageWithSingleType(OBJECT_TYPE),
                        UtAddrExpression(touchedArrayExpression.select(mkInt(i)))
                    )
                )
            })

        val storageFieldChunkId = resolver.hierarchy.chunkIdForField(wrapper.type, storageField)
        val storageFieldArrayDescriptor = MemoryChunkDescriptor(
            storageFieldChunkId,
            wrapper.type,
            OBJECT_TYPE.arrayElementType
        )

        val storageArrayAddr = resolver.findArray(storageFieldArrayDescriptor, MemoryState.CURRENT).select(wrapper.addr)
        val storageArrayExpression = resolver.findArray(arrayValuesDescriptor, resolver.state).select(storageArrayAddr)

        val storageValues = UtArrayModel(
            resolver.holder.concreteAddr(UtAddrExpression(storageArrayAddr)),
            objectArrayClassId,
            sizeValue,
            UtNullModel(objectClassId),
            stores = (0 until sizeValue).associateTo(mutableMapOf()) { i ->
                val model = touchedValues.stores[i]
                val addr = model.getIdOrThrow()
                addr to resolver.resolveModel(
                    ObjectValue(
                        TypeStorage.constructTypeStorageWithSingleType(OBJECT_TYPE),
                        UtAddrExpression(storageArrayExpression.select(mkInt(addr)))
                    )
                )
            })

        val model = UtCompositeModel(resolver.holder.concreteAddr(wrapper.addr), associativeArrayId, isMock = false)

        model.fields[sizeField.fieldId] = UtPrimitiveModel(sizeValue)
        model.fields[touchedField.fieldId] = touchedValues
        model.fields[storageField.fieldId] = storageValues

        return model
    }

    private fun Traverser.getStorageArrayField(addr: UtAddrExpression) =
        getArrayField(addr, associativeArrayClass, storageField)

    private fun Traverser.getTouchedArrayField(addr: UtAddrExpression) =
        getArrayField(addr, associativeArrayClass, touchedField)

    private fun Traverser.getTouchedArrayExpression(wrapper: ObjectValue): UtExpression =
        selectArrayExpressionFromMemory(getTouchedArrayField(wrapper.addr))

    private fun Traverser.getStorageArrayExpression(
        wrapper: ObjectValue
    ): UtExpression = selectArrayExpressionFromMemory(getStorageArrayField(wrapper.addr))

    override val selectOperationTypeIndex: Int
        get() = 1
}
