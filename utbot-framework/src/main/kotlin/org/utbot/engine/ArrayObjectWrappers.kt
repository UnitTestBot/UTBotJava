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
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.getIdOrThrow
import org.utbot.framework.plugin.api.idOrNull
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import soot.ArrayType
import soot.Scene
import soot.SootClass
import soot.SootField
import soot.SootMethod

val rangeModifiableArrayId: ClassId = RangeModifiableUnlimitedArray::class.id

class RangeModifiableUnlimitedArrayWrapper : WrapperInterface {
    override fun Traverser.invoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult> {
        return when (method.name) {
            "<init>" -> {
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
            "insert" -> {
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
            "insertRange" -> {
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
            "remove" -> {
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
            "removeRange" -> {
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
            "set" -> {
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
            "get" -> {
                val value = getStorageArrayExpression(wrapper).select((parameters[0] as PrimitiveValue).expr)
                val addr = UtAddrExpression(value)
                val resultObject = createObject(addr, OBJECT_TYPE, useConcreteType = false)

                listOf(
                    MethodResult(
                        SymbolicSuccess(resultObject),
                        typeRegistry.typeConstraintToGenericTypeParameter(addr, wrapper.addr, i = TYPE_PARAMETER_INDEX)
                            .asHardConstraint()
                    )
                )
            }
            "toArray" -> {
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
            "setRange" -> {
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
            else -> error("unknown method ${method.name} for ${javaClass.simpleName} class")
        }
    }

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
        val typeStorage =
            resolver.typeRegistry.getTypeStoragesForObjectTypeParameters(wrapper.addr)?.singleOrNull() ?: TypeRegistry.objectTypeStorage

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


    override fun Traverser.invoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult> {
        return when (method.name) {
            "<init>" -> {
                val storageArrayAddr = findNewAddr()
                val touchedArrayAddr = findNewAddr()

                listOf(
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
            "select" -> {
                val value = getStorageArrayExpression(wrapper).select(parameters[0].addr)
                val addr = UtAddrExpression(value)
                val resultObject = createObject(addr, OBJECT_TYPE, useConcreteType = false)

                listOf(
                    MethodResult(
                        SymbolicSuccess(resultObject),
                        typeRegistry.typeConstraintToGenericTypeParameter(
                            addr,
                            wrapper.addr,
                            TYPE_PARAMETER_INDEX
                        ).asHardConstraint()
                    )
                )
            }
            "store" -> {
                val storageValue = getStorageArrayExpression(wrapper).store(parameters[0].addr, parameters[1].addr)
                val sizeValue = getIntFieldValue(wrapper, sizeField)
                val touchedValue = getTouchedArrayExpression(wrapper).store(sizeValue, parameters[0].addr)
                listOf(
                    MethodResult(
                        SymbolicSuccess(voidValue),
                        memoryUpdates = arrayUpdateWithValue(
                            getStorageArrayField(wrapper.addr).addr,
                            OBJECT_TYPE.arrayType,
                            storageValue
                        ) + arrayUpdateWithValue(
                            getTouchedArrayField(wrapper.addr).addr,
                            OBJECT_TYPE.arrayType,
                            touchedValue,
                        ) + objectUpdate(wrapper, sizeField, Add(sizeValue.toIntValue(), 1.toPrimitiveValue()))
                    )
                )
            }
            else -> error("unknown method ${method.name} for AssociativeArray class")
        }
    }

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
            objectClassId,
            sizeValue,
            UtNullModel(objectClassId),
            stores = (0 until sizeValue).associateWithTo(mutableMapOf()) { i ->
                resolver.resolveModel(
                    ObjectValue(
                        TypeStorage(OBJECT_TYPE),
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
            objectClassId,
            sizeValue,
            UtNullModel(objectClassId),
            stores = (0 until sizeValue).associateTo(mutableMapOf()) { i ->
                val model = touchedValues.stores[i]
                val addr = model.getIdOrThrow()
                addr to resolver.resolveModel(
                    ObjectValue(
                        TypeStorage(OBJECT_TYPE),
                        UtAddrExpression(storageArrayExpression.select(mkInt(addr)))
                    )
                )
            })

        val model = UtCompositeModel(resolver.holder.concreteAddr(wrapper.addr), associativeArrayId, false)
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
}

// Arrays and lists have the only type parameter with index zero
private const val TYPE_PARAMETER_INDEX = 0
