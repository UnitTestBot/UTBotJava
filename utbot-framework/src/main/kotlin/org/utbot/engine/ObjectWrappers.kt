package org.utbot.engine

import org.utbot.common.WorkaroundReason.MAKE_SYMBOLIC
import org.utbot.common.workaround
import org.utbot.engine.UtListClass.UT_ARRAY_LIST
import org.utbot.engine.UtListClass.UT_LINKED_LIST
import org.utbot.engine.UtListClass.UT_LINKED_LIST_WITH_NULLABLE_CHECK
import org.utbot.engine.UtOptionalClass.UT_OPTIONAL
import org.utbot.engine.UtOptionalClass.UT_OPTIONAL_DOUBLE
import org.utbot.engine.UtOptionalClass.UT_OPTIONAL_INT
import org.utbot.engine.UtOptionalClass.UT_OPTIONAL_LONG
import org.utbot.engine.UtStreamClass.UT_DOUBLE_STREAM
import org.utbot.engine.UtStreamClass.UT_INT_STREAM
import org.utbot.engine.UtStreamClass.UT_LONG_STREAM
import org.utbot.engine.UtStreamClass.UT_STREAM
import org.utbot.engine.overrides.collections.AssociativeArray
import org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray
import org.utbot.engine.overrides.collections.UtHashMap
import org.utbot.engine.overrides.collections.UtHashSet
import org.utbot.engine.overrides.security.UtSecurityManager
import org.utbot.engine.overrides.strings.UtString
import org.utbot.engine.overrides.strings.UtStringBuffer
import org.utbot.engine.overrides.strings.UtStringBuilder
import org.utbot.engine.overrides.threads.UtCompletableFuture
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.util.nextModelName
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.SootMethod
import java.util.Optional
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import kotlin.reflect.KClass

typealias TypeToBeWrapped = RefType
typealias WrapperType = RefType

/**
 * Contains mapping from RefTypes of the classes from the standard library
 * in corresponding RefType of our own implemented wrapper classes.
 */
val classToWrapper: MutableMap<TypeToBeWrapped, WrapperType> =
    mutableMapOf<TypeToBeWrapped, WrapperType>().apply {
        putSootClass(java.lang.StringBuilder::class, utStringBuilderClass)
        putSootClass(UtStringBuilder::class, utStringBuilderClass)
        putSootClass(java.lang.StringBuffer::class, utStringBufferClass)
        putSootClass(UtStringBuffer::class, utStringBufferClass)
        putSootClass(java.lang.CharSequence::class, utStringClass)
        putSootClass(java.lang.String::class, utStringClass)
        putSootClass(UtString::class, utStringClass)
        putSootClass(Optional::class, UT_OPTIONAL.className)
        putSootClass(OptionalInt::class, UT_OPTIONAL_INT.className)
        putSootClass(OptionalLong::class, UT_OPTIONAL_LONG.className)
        putSootClass(OptionalDouble::class, UT_OPTIONAL_DOUBLE.className)

        // threads
        putSootClass(java.lang.Thread::class, utThreadClass)
        putSootClass(java.lang.ThreadGroup::class, utThreadGroupClass)

        // executors, futures and latches
        putSootClass(ExecutorService::class, utExecutorServiceClass)
        putSootClass(ThreadPoolExecutor::class, utExecutorServiceClass)
        putSootClass(ForkJoinPool::class, utExecutorServiceClass)
        putSootClass(ScheduledThreadPoolExecutor::class, utExecutorServiceClass)
        putSootClass(CountDownLatch::class, utCountDownLatchClass)
        putSootClass(CompletableFuture::class, utCompletableFutureClass)
        putSootClass(CompletionStage::class, utCompletableFutureClass)
        // A hack to be able to create UtCompletableFuture in its methods as a wrapper
        putSootClass(UtCompletableFuture::class, utCompletableFutureClass)

        putSootClass(RangeModifiableUnlimitedArray::class, RangeModifiableUnlimitedArrayWrapper::class)
        putSootClass(AssociativeArray::class, AssociativeArrayWrapper::class)

        putSootClass(java.util.List::class, UT_ARRAY_LIST.className)
        putSootClass(java.util.AbstractList::class, UT_ARRAY_LIST.className)
        putSootClass(java.util.ArrayList::class, UT_ARRAY_LIST.className)
        putSootClass(CopyOnWriteArrayList::class, UT_ARRAY_LIST.className)
        putSootClass(java.util.LinkedList::class, UT_LINKED_LIST.className)
        putSootClass(java.util.AbstractSequentialList::class, UT_LINKED_LIST.className)

        putSootClass(java.util.ArrayDeque::class, UT_LINKED_LIST_WITH_NULLABLE_CHECK.className)
        putSootClass(java.util.concurrent.ConcurrentLinkedDeque::class, UT_LINKED_LIST_WITH_NULLABLE_CHECK.className)
        putSootClass(java.util.concurrent.ConcurrentLinkedQueue::class, UT_LINKED_LIST_WITH_NULLABLE_CHECK.className)
        putSootClass(java.util.concurrent.LinkedBlockingDeque::class, UT_LINKED_LIST_WITH_NULLABLE_CHECK.className)
        putSootClass(java.util.concurrent.LinkedBlockingQueue::class, UT_LINKED_LIST_WITH_NULLABLE_CHECK.className)

        putSootClass(java.util.Set::class, UtHashSet::class)
        putSootClass(java.util.AbstractSet::class, UtHashSet::class)
        putSootClass(java.util.HashSet::class, UtHashSet::class)
        putSootClass(java.util.LinkedHashSet::class, UtHashSet::class)

        putSootClass(java.util.Map::class, UtHashMap::class)
        putSootClass(java.util.AbstractMap::class, UtHashMap::class)
        putSootClass(java.util.LinkedHashMap::class, UtHashMap::class)
        putSootClass(java.util.HashMap::class, UtHashMap::class)
        putSootClass(java.util.concurrent.ConcurrentHashMap::class, UtHashMap::class)

        putSootClass(java.util.stream.BaseStream::class, UT_STREAM.className)
        putSootClass(java.util.stream.Stream::class, UT_STREAM.className)
        putSootClass(java.util.stream.IntStream::class, UT_INT_STREAM.className)
        putSootClass(java.util.stream.LongStream::class, UT_LONG_STREAM.className)
        putSootClass(java.util.stream.DoubleStream::class, UT_DOUBLE_STREAM.className)

        putSootClass(java.lang.SecurityManager::class, UtSecurityManager::class)
    }

/**
 * For each [WrapperType] returns a set of types for which it is a wrapper.
 *
 * Note that it and [classToWrapper] cannot be BiMap since one wrapper can substitute several real classes.
 */
val wrapperToClass: Map<WrapperType, Set<TypeToBeWrapped>> =
    mutableMapOf<WrapperType, MutableSet<TypeToBeWrapped>>().apply {
        classToWrapper.entries.forEach {
            getOrPut(it.value) { mutableSetOf() } += it.key
        }
    }

private fun MutableMap<TypeToBeWrapped, WrapperType>.putSootClass(
    key: KClass<*>,
    value: KClass<*>
) = putSootClass(key, Scene.v().getSootClass(value.java.canonicalName).type)

private fun MutableMap<TypeToBeWrapped, WrapperType>.putSootClass(
    key: KClass<*>,
    valueName: String
) = putSootClass(key, Scene.v().getSootClass(valueName).type)

private fun MutableMap<TypeToBeWrapped, WrapperType>.putSootClass(
    key: KClass<*>,
    value: SootClass
) = putSootClass(key, value.type)

private fun MutableMap<TypeToBeWrapped, WrapperType>.putSootClass(
    key: KClass<*>,
    value: RefType
) = put(Scene.v().getSootClass(key.java.canonicalName).type, value)

private val wrappers = mapOf(
    wrap(java.lang.StringBuilder::class) { type, addr -> objectValue(type, addr, UtStringBuilderWrapper()) },
    wrap(UtStringBuilder::class) { type, addr -> objectValue(type, addr, UtStringBuilderWrapper()) },
    wrap(java.lang.StringBuffer::class) { type, addr -> objectValue(type, addr, UtStringBufferWrapper()) },
    wrap(UtStringBuffer::class) { type, addr -> objectValue(type, addr, UtStringBufferWrapper()) },
    wrap(java.lang.CharSequence::class) { type, addr -> objectValue(type, addr, StringWrapper()) },
    wrap(java.lang.String::class) { type, addr -> objectValue(type, addr, StringWrapper()) },
    wrap(UtString::class) { type, addr -> objectValue(type, addr, StringWrapper()) },
    wrap(Optional::class) { type, addr -> objectValue(type, addr, OptionalWrapper(UT_OPTIONAL)) },
    wrap(OptionalInt::class) { type, addr -> objectValue(type, addr, OptionalWrapper(UT_OPTIONAL_INT)) },
    wrap(OptionalLong::class) { type, addr -> objectValue(type, addr, OptionalWrapper(UT_OPTIONAL_LONG)) },
    wrap(OptionalDouble::class) { type, addr -> objectValue(type, addr, OptionalWrapper(UT_OPTIONAL_DOUBLE)) },

    // threads
    wrap(Thread::class) { type, addr -> objectValue(type, addr, ThreadWrapper()) },
    wrap(ThreadGroup::class) { type, addr -> objectValue(type, addr, ThreadGroupWrapper()) },
    wrap(ExecutorService::class) { type, addr -> objectValue(type, addr, ExecutorServiceWrapper()) },
    wrap(ThreadPoolExecutor::class) { type, addr -> objectValue(type, addr, ExecutorServiceWrapper()) },
    wrap(ForkJoinPool::class) { type, addr -> objectValue(type, addr, ExecutorServiceWrapper()) },
    wrap(ScheduledThreadPoolExecutor::class) { type, addr -> objectValue(type, addr, ExecutorServiceWrapper()) },
    wrap(CountDownLatch::class) { type, addr -> objectValue(type, addr, CountDownLatchWrapper()) },
    wrap(CompletableFuture::class) { type, addr -> objectValue(type, addr, CompletableFutureWrapper()) },
    wrap(CompletionStage::class) { type, addr -> objectValue(type, addr, CompletableFutureWrapper()) },
    // A hack to be able to create UtCompletableFuture in its methods as a wrapper
    wrap(UtCompletableFuture::class) { type, addr -> objectValue(type, addr, CompletableFutureWrapper()) },

    wrap(RangeModifiableUnlimitedArray::class) { type, addr ->
        objectValue(type, addr, RangeModifiableUnlimitedArrayWrapper())
    },
    wrap(AssociativeArray::class) { type, addr ->
        objectValue(type, addr, AssociativeArrayWrapper())
    },

    // list wrappers
    wrap(java.util.List::class) { _, addr -> objectValue(ARRAY_LIST_TYPE, addr, ListWrapper(UT_ARRAY_LIST)) },
    wrap(java.util.AbstractList::class) { _, addr -> objectValue(ARRAY_LIST_TYPE, addr, ListWrapper(UT_ARRAY_LIST)) },
    wrap(java.util.ArrayList::class) { _, addr -> objectValue(ARRAY_LIST_TYPE, addr, ListWrapper(UT_ARRAY_LIST)) },


    wrap(CopyOnWriteArrayList::class) { type, addr -> objectValue(type, addr, ListWrapper(UT_ARRAY_LIST)) },

    wrap(java.util.LinkedList::class) { _, addr -> objectValue(LINKED_LIST_TYPE, addr, ListWrapper(UT_LINKED_LIST)) },
    wrap(java.util.AbstractSequentialList::class) { _, addr -> objectValue(LINKED_LIST_TYPE, addr, ListWrapper(UT_LINKED_LIST)) },

    // queue, deque wrappers
    wrap(java.util.ArrayDeque::class) { type, addr ->
        objectValue(type, addr, ListWrapper(UT_LINKED_LIST_WITH_NULLABLE_CHECK))
    },
    wrap(java.util.concurrent.ConcurrentLinkedDeque::class) { type, addr ->
        objectValue(type, addr, ListWrapper(UT_LINKED_LIST_WITH_NULLABLE_CHECK))
    },
    wrap(java.util.concurrent.ConcurrentLinkedQueue::class) { type, addr ->
        objectValue(type, addr, ListWrapper(UT_LINKED_LIST_WITH_NULLABLE_CHECK))
    },
    wrap(java.util.concurrent.LinkedBlockingDeque::class) { type, addr ->
        objectValue(type, addr, ListWrapper(UT_LINKED_LIST_WITH_NULLABLE_CHECK))
    },
    wrap(java.util.concurrent.LinkedBlockingQueue::class) { type, addr ->
        objectValue(type, addr, ListWrapper(UT_LINKED_LIST_WITH_NULLABLE_CHECK))
    },

    // set wrappers
    wrap(java.util.Set::class) { _, addr -> objectValue(LINKED_HASH_SET_TYPE, addr, SetWrapper()) },
    wrap(java.util.AbstractSet::class) { _, addr -> objectValue(LINKED_HASH_SET_TYPE, addr, SetWrapper()) },
    wrap(java.util.HashSet::class) { _, addr -> objectValue(HASH_SET_TYPE, addr, SetWrapper()) },
    wrap(java.util.LinkedHashSet::class) { _, addr -> objectValue(LINKED_HASH_SET_TYPE, addr, SetWrapper()) },

    // map wrappers
    wrap(java.util.Map::class) { _, addr -> objectValue(LINKED_HASH_MAP_TYPE, addr, MapWrapper()) },
    wrap(java.util.AbstractMap::class) { _, addr -> objectValue(LINKED_HASH_MAP_TYPE, addr, MapWrapper()) },
    wrap(java.util.LinkedHashMap::class) { _, addr -> objectValue(LINKED_HASH_MAP_TYPE, addr, MapWrapper()) },
    wrap(java.util.HashMap::class) { _, addr -> objectValue(HASH_MAP_TYPE, addr, MapWrapper()) },
    wrap(java.util.concurrent.ConcurrentHashMap::class) { _, addr -> objectValue(HASH_MAP_TYPE, addr, MapWrapper()) },

    // stream wrappers
    wrap(java.util.stream.BaseStream::class) { _, addr -> objectValue(STREAM_TYPE, addr, CommonStreamWrapper()) },
    wrap(java.util.stream.Stream::class) { _, addr -> objectValue(STREAM_TYPE, addr, CommonStreamWrapper()) },
    wrap(java.util.stream.IntStream::class) { _, addr -> objectValue(INT_STREAM_TYPE, addr, IntStreamWrapper()) },
    wrap(java.util.stream.LongStream::class) { _, addr -> objectValue(LONG_STREAM_TYPE, addr, LongStreamWrapper()) },
    wrap(java.util.stream.DoubleStream::class) { _, addr -> objectValue(DOUBLE_STREAM_TYPE, addr, DoubleStreamWrapper()) },

    // Security-related wrappers
    wrap(SecurityManager::class) { type, addr -> objectValue(type, addr, SecurityManagerWrapper()) },
).also {
    // check every `wrapped` class has a corresponding value in [classToWrapper]
    val missedWrappers = it.keys.filterNot { key ->
        Scene.v().getSootClass(key.name).type in classToWrapper.keys
    }

    require(missedWrappers.isEmpty()) {
        "Missed wrappers for classes [${missedWrappers.joinToString(", ")}]"
    }
}

private fun wrap(kClass: KClass<*>, implementation: (RefType, UtAddrExpression) -> ObjectValue) =
    kClass.id to implementation

internal fun wrapper(type: RefType, addr: UtAddrExpression): ObjectValue? =
    wrappers[type.id]?.invoke(type, addr)

typealias MethodSymbolicImplementation = (Traverser, ObjectValue, SootMethod, List<SymbolicValue>) -> List<MethodResult>

interface WrapperInterface {
    /**
     * Checks whether a symbolic implementation exists for the [method].
     */
    fun isWrappedMethod(method: SootMethod): Boolean = method.name in wrappedMethods

    /**
     * Mapping from a method signature to its symbolic implementation (if present).
     */
    val wrappedMethods: Map<String, MethodSymbolicImplementation>

    /**
     * Returns list of invocation results
     */
    operator fun Traverser.invoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult> {
        val wrappedMethodResult = wrappedMethods[method.name]
            ?: error("unknown wrapped method ${method.name} for ${this@WrapperInterface::class}")

        return wrappedMethodResult(this, wrapper, method, parameters)
    }

    fun value(resolver: Resolver, wrapper: ObjectValue): UtModel

    /**
     * It is an index for type parameter corresponding to the result
     * value of `select` operation. For example, for arrays and lists it's zero,
     * for associative array it's one.
     */
    val selectOperationTypeIndex: Int
        get() = 0

    /**
     * Similar to [selectOperationTypeIndex], it is responsible for type index
     * of the returning value from `get` operation
     */
    val getOperationTypeIndex: Int
        get() = 0
}

// TODO: perhaps we have to have wrapper around concrete value here
data class ThrowableWrapper(val throwable: Throwable) : WrapperInterface {
    override val wrappedMethods: Map<String, MethodSymbolicImplementation> = emptyMap()

    override fun isWrappedMethod(method: SootMethod): Boolean = true

    override fun Traverser.invoke(wrapper: ObjectValue, method: SootMethod, parameters: List<SymbolicValue>) =
        workaround(MAKE_SYMBOLIC) {
            listOf(
                MethodResult(
                    createConst(method.returnType, typeRegistry.findNewSymbolicReturnValueName())
                )
            )
        }

    // TODO: for now we do not build throwable.cause model, but later we should do it
    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel {
        val classId = throwable.javaClass.id
        val addr = resolver.holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName(throwable.javaClass.simpleName.decapitalize())

        val instantiationCall = when (val message = throwable.message) {
            null -> UtExecutableCallModel(instance = null, constructorId(classId), emptyList())
            else -> UtExecutableCallModel(
                instance = null,
                constructorId(classId, stringClassId),
                listOf(UtPrimitiveModel(message))
            )
        }

        return UtAssembleModel(addr, classId, modelName, instantiationCall)
    }
}