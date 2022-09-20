package org.utbot.engine

import com.github.curiousoddman.rgxgen.RgxGen
import org.utbot.common.unreachableBranch
import org.utbot.engine.overrides.strings.UtNativeString
import org.utbot.engine.overrides.strings.UtString
import org.utbot.engine.overrides.strings.UtStringBuffer
import org.utbot.engine.overrides.strings.UtStringBuilder
import org.utbot.engine.pc.RewritingVisitor
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.UtConvertToString
import org.utbot.engine.pc.UtFalse
import org.utbot.engine.pc.UtIntSort
import org.utbot.engine.pc.UtLongSort
import org.utbot.engine.pc.UtStringCharAt
import org.utbot.engine.pc.UtStringLength
import org.utbot.engine.pc.UtStringToArray
import org.utbot.engine.pc.UtStringToInt
import org.utbot.engine.pc.UtTrue
import org.utbot.engine.pc.cast
import org.utbot.engine.pc.isConcrete
import org.utbot.engine.pc.mkAnd
import org.utbot.engine.pc.mkChar
import org.utbot.engine.pc.mkEq
import org.utbot.engine.pc.mkInt
import org.utbot.engine.pc.mkNot
import org.utbot.engine.pc.mkString
import org.utbot.engine.pc.select
import org.utbot.engine.pc.toConcrete
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.charArrayClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.util.nextModelName
import kotlin.math.max
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import soot.CharType
import soot.IntType
import soot.Scene
import soot.SootClass
import soot.SootField
import soot.SootMethod

val utStringClass: SootClass
    get() = Scene.v().getSootClass(UtString::class.qualifiedName)

class StringWrapper : BaseOverriddenWrapper(utStringClass.name) {
    private val toStringMethodSignature =
        overriddenClass.getMethodByName(UtString::toStringImpl.name).subSignature
    private val matchesMethodSignature =
        overriddenClass.getMethodByName(UtString::matchesImpl.name).subSignature
    private val charAtMethodSignature =
        overriddenClass.getMethodByName(UtString::charAtImpl.name).subSignature

    private fun Traverser.getValueArray(addr: UtAddrExpression) =
        getArrayField(addr, overriddenClass, STRING_VALUE)

    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? {
        return when (method.subSignature) {
            toStringMethodSignature -> {
                listOf(MethodResult(wrapper.copy(typeStorage = TypeStorage(method.returnType))))
            }
            matchesMethodSignature -> {
                val arg = parameters[0] as ObjectValue
                val matchingLengthExpr = getIntFieldValue(arg, STRING_LENGTH).accept(RewritingVisitor())

                if (!matchingLengthExpr.isConcrete) return null

                val matchingValueExpr =
                    selectArrayExpressionFromMemory(getValueArray(arg.addr)).accept(RewritingVisitor())
                val matchingLength = matchingLengthExpr.toConcrete() as Int
                val matchingValue = CharArray(matchingLength)

                for (i in 0 until matchingLength) {
                    val charExpr = matchingValueExpr.select(mkInt(i)).accept(RewritingVisitor())

                    if (!charExpr.isConcrete) return null

                    matchingValue[i] = (charExpr.toConcrete() as Number).toChar()
                }

                val rgxGen = RgxGen(String(matchingValue))
                val matching = (rgxGen.generate())
                val notMatching = rgxGen.generateNotMatching()

                val thisLength = getIntFieldValue(wrapper, STRING_LENGTH)
                val thisValue = selectArrayExpressionFromMemory(getValueArray(wrapper.addr))

                val matchingConstraints = mutableSetOf<UtBoolExpression>()
                matchingConstraints += mkEq(thisLength, mkInt(matching.length))
                for (i in matching.indices) {
                    matchingConstraints += mkEq(thisValue.select(mkInt(i)), mkChar(matching[i]))
                }

                val notMatchingConstraints = mutableSetOf<UtBoolExpression>()
                notMatchingConstraints += mkEq(thisLength, mkInt(notMatching.length))
                for (i in notMatching.indices) {
                    notMatchingConstraints += mkEq(thisValue.select(mkInt(i)), mkChar(notMatching[i]))
                }

                return listOf(
                    MethodResult(UtTrue.toBoolValue(), matchingConstraints.asHardConstraint()),
                    MethodResult(UtFalse.toBoolValue(), notMatchingConstraints.asHardConstraint())
                )
            }
            charAtMethodSignature -> {
                val index = parameters[0] as PrimitiveValue
                val lengthExpr = getIntFieldValue(wrapper, STRING_LENGTH)
                val inBoundsCondition = mkAnd(Le(0.toPrimitiveValue(), index), Lt(index, lengthExpr.toIntValue()))
                val failMethodResult =
                    MethodResult(
                        explicitThrown(
                            StringIndexOutOfBoundsException(),
                            findNewAddr(),
                            environment.state.isInNestedMethod()
                        ),
                        hardConstraints = mkNot(inBoundsCondition).asHardConstraint()
                    )

                val valueExpr = selectArrayExpressionFromMemory(getValueArray(wrapper.addr))

                val returnResult = MethodResult(
                    valueExpr.select(index.expr).toCharValue(),
                    hardConstraints = inBoundsCondition.asHardConstraint()
                )
                return listOf(returnResult, failMethodResult)
            }
            else -> return null
        }
    }

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel = resolver.run {
        val classId = STRING_TYPE.id
        val addr = holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName("string")

        val charType = CharType.v()
        val charArrayType = charType.arrayType

        val arrayValuesChunkId = resolver.typeRegistry.arrayChunkId(charArrayType)

        val valuesFieldChunkId = resolver.hierarchy.chunkIdForField(utStringClass.type, STRING_VALUE)
        val valuesFieldChunkDescriptor = MemoryChunkDescriptor(valuesFieldChunkId, wrapper.type, charArrayType)
        val valuesArrayAddr = resolver.findArray(valuesFieldChunkDescriptor, MemoryState.CURRENT).select(wrapper.addr)

        val valuesArrayDescriptor = MemoryChunkDescriptor(arrayValuesChunkId, charArrayType, charType)
        val valuesArray = resolver.findArray(valuesArrayDescriptor, resolver.state)
        val valuesArrayExpression = valuesArray.select(valuesArrayAddr)

        val length = max(0, resolveIntField(wrapper, STRING_LENGTH))

        val values = UtArrayModel(
            holder.concreteAddr(wrapper.addr),
            charArrayClassId,
            length,
            charClassId.defaultValueModel(),
            stores = (0 until length).associateWithTo(mutableMapOf()) { i ->
                resolver.resolveModel(
                    valuesArrayExpression.select(mkInt(i)).toCharValue()
                )
            })

        val charValues = CharArray(length) { (values.stores[it] as UtPrimitiveModel).value as Char }
        val stringModel = UtPrimitiveModel(String(charValues))

        val instantiationCall = UtExecutableCallModel(
            instance = null,
            constructorId(classId, STRING_TYPE.classId),
            listOf(stringModel)
        )
        return UtAssembleModel(addr, classId, modelName, instantiationCall)
    }
}

internal val utNativeStringClass = Scene.v().getSootClass(UtNativeString::class.qualifiedName)

private var stringNameIndex = 0
private fun nextStringName() = "\$string${stringNameIndex++}"

class UtNativeStringWrapper : WrapperInterface {
    private val valueDescriptor = NATIVE_STRING_VALUE_DESCRIPTOR
    override fun Traverser.invoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult> =
        when (method.subSignature) {
            "void <init>()" -> {
                val newString = mkString(nextStringName())

                val memoryUpdate = MemoryUpdate(
                    stores = persistentListOf(simplifiedNamedStore(valueDescriptor, wrapper.addr, newString)),
                    touchedChunkDescriptors = persistentSetOf(valueDescriptor)
                )
                listOf(
                    MethodResult(
                        SymbolicSuccess(voidValue),
                        memoryUpdates = memoryUpdate
                    )
                )
            }
            "void <init>(int)" -> {
                val newString = UtConvertToString((parameters[0] as PrimitiveValue).expr)
                val memoryUpdate = MemoryUpdate(
                    stores = persistentListOf(simplifiedNamedStore(valueDescriptor, wrapper.addr, newString)),
                    touchedChunkDescriptors = persistentSetOf(valueDescriptor)
                )
                listOf(
                    MethodResult(
                        SymbolicSuccess(voidValue),
                        memoryUpdates = memoryUpdate
                    )
                )
            }
            "void <init>(long)" -> {
                val newString = UtConvertToString((parameters[0] as PrimitiveValue).expr)
                val memoryUpdate = MemoryUpdate(
                    stores = persistentListOf(simplifiedNamedStore(valueDescriptor, wrapper.addr, newString)),
                    touchedChunkDescriptors = persistentSetOf(valueDescriptor)
                )
                listOf(
                    MethodResult(
                        SymbolicSuccess(voidValue),
                        memoryUpdates = memoryUpdate
                    )
                )
            }
            "int length()" -> {
                val result = UtStringLength(memory.nativeStringValue(wrapper.addr))
                listOf(MethodResult(SymbolicSuccess(result.toByteValue().cast(IntType.v()))))
            }
            "char charAt(int)" -> {
                val index = (parameters[0] as PrimitiveValue).expr
                val result = UtStringCharAt(memory.nativeStringValue(wrapper.addr), index)
                listOf(MethodResult(SymbolicSuccess(result.toCharValue())))
            }
            "int codePointAt(int)" -> {
                val index = (parameters[0] as PrimitiveValue).expr
                val result = UtStringCharAt(memory.nativeStringValue(wrapper.addr), index)
                listOf(MethodResult(SymbolicSuccess(result.toCharValue().cast(IntType.v()))))
            }
            "int toInteger()" -> {
                val result = UtStringToInt(memory.nativeStringValue(wrapper.addr), UtIntSort)
                listOf(MethodResult(SymbolicSuccess(result.toIntValue())))
            }
            "long toLong()" -> {
                val result = UtStringToInt(memory.nativeStringValue(wrapper.addr), UtLongSort)
                listOf(MethodResult(SymbolicSuccess(result.toLongValue())))
            }
            "char[] toCharArray(int)" -> {
                val stringExpression = memory.nativeStringValue(wrapper.addr)
                val result = UtStringToArray(stringExpression, parameters[0] as PrimitiveValue)
                val length = UtStringLength(stringExpression)
                val type = CharType.v()
                val arrayType = type.arrayType
                val arrayValue = createNewArray(length.toIntValue(), arrayType, type)
                listOf(
                    MethodResult(
                        SymbolicSuccess(arrayValue),
                        memoryUpdates = arrayUpdateWithValue(arrayValue.addr, arrayType, result)
                    )
                )
            }
            else -> unreachableBranch("Unknown signature at the NativeStringWrapper.invoke: ${method.signature}")
        }

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel = UtNullModel(STRING_TYPE.classId)
}

sealed class UtAbstractStringBuilderWrapper(className: String) : BaseOverriddenWrapper(className) {
    private val asStringBuilderMethodSignature =
        overriddenClass.getMethodByName("asStringBuilder").subSignature

    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? {
        if (method.subSignature == asStringBuilderMethodSignature) {
            return listOf(MethodResult(wrapper.copy(typeStorage = TypeStorage(method.returnType))))
        }

        return null
    }

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel = resolver.run {
        val addr = holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName("stringBuilder")

        val charType = CharType.v()
        val charArrayType = charType.arrayType

        val arrayValuesChunkId = typeRegistry.arrayChunkId(charArrayType)

        val valuesFieldChunkId = hierarchy.chunkIdForField(overriddenClass.type, overriddenClass.valueField)
        val valuesArrayAddrDescriptor = MemoryChunkDescriptor(valuesFieldChunkId, wrapper.type, charType)
        val valuesArrayAddr = findArray(valuesArrayAddrDescriptor, MemoryState.CURRENT).select(wrapper.addr)

        val valuesArrayDescriptor = MemoryChunkDescriptor(arrayValuesChunkId, charArrayType, charType)
        val valuesArrayExpression = findArray(valuesArrayDescriptor, state).select(valuesArrayAddr)

        val length = resolveIntField(wrapper, overriddenClass.countField)

        val values = UtArrayModel(
            holder.concreteAddr(wrapper.addr),
            charArrayClassId,
            length,
            charClassId.defaultValueModel(),
            stores = (0 until length).associateWithTo(mutableMapOf()) { i ->
                resolver.resolveModel(valuesArrayExpression.select(mkInt(i)).toCharValue())
            })

        val charValues = CharArray(length) { (values.stores[it] as UtPrimitiveModel).value as Char }
        val stringModel = UtPrimitiveModel(String(charValues))
        val constructorId = constructorId(wrapper.type.classId, STRING_TYPE.classId)
        val instantiationCall = UtExecutableCallModel(
            instance = null,
            constructorId,
            listOf(stringModel)
        )
        return UtAssembleModel(addr, wrapper.type.classId, modelName, instantiationCall)
    }

    private val SootClass.valueField: SootField
        get() = getField("value", CharType.v().arrayType)

    private val SootClass.countField: SootField
        get() = getField("count", IntType.v())
}

val utStringBuilderClass: SootClass
    get() = Scene.v().getSootClass(UtStringBuilder::class.qualifiedName)

class UtStringBuilderWrapper : UtAbstractStringBuilderWrapper(utStringBuilderClass.name)

val utStringBufferClass: SootClass
    get() = Scene.v().getSootClass(UtStringBuffer::class.qualifiedName)

class UtStringBufferWrapper : UtAbstractStringBuilderWrapper(utStringBufferClass.name)