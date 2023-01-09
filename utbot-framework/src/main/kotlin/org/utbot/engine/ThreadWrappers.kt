package org.utbot.engine

import org.utbot.engine.overrides.threads.UtCompletableFuture
import org.utbot.engine.overrides.threads.UtCountDownLatch
import org.utbot.engine.overrides.threads.UtExecutorService
import org.utbot.engine.overrides.threads.UtThread
import org.utbot.engine.overrides.threads.UtThreadGroup
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.types.COMPLETABLE_FUTURE_TYPE
import org.utbot.engine.types.COUNT_DOWN_LATCH_TYPE
import org.utbot.engine.types.EXECUTORS_TYPE
import org.utbot.engine.types.OBJECT_TYPE
import org.utbot.engine.types.STRING_TYPE
import org.utbot.engine.types.THREAD_GROUP_TYPE
import org.utbot.engine.types.THREAD_TYPE
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.util.executableId
import org.utbot.framework.util.nextModelName
import soot.IntType
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.SootMethod

val utThreadClass: SootClass
    get() = Scene.v().getSootClass(UtThread::class.qualifiedName)
val utThreadGroupClass: SootClass
    get() = Scene.v().getSootClass(UtThreadGroup::class.qualifiedName)
val utCompletableFutureClass: SootClass
    get() = Scene.v().getSootClass(UtCompletableFuture::class.qualifiedName)
val utExecutorServiceClass: SootClass
    get() = Scene.v().getSootClass(UtExecutorService::class.qualifiedName)
val utCountDownLatchClass: SootClass
    get() = Scene.v().getSootClass(UtCountDownLatch::class.qualifiedName)

class ThreadWrapper : BaseOverriddenWrapper(utThreadClass.name) {
    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? = null

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel =
        resolver.run {
            val classId = THREAD_TYPE.id
            val addr = holder.concreteAddr(wrapper.addr)
            val modelName = nextModelName("thread")

            val values = collectFieldModels(wrapper.addr, overriddenClass.type)
            val targetModel = values[targetFieldId] as? UtLambdaModel

            val (constructor, params) = if (targetModel == null) {
                constructorId(classId) to emptyList()
            } else {
                constructorId(classId, runnableType.id) to listOf(targetModel)
            }

            val instantiationCall = UtExecutableCallModel(
                instance = null,
                constructor,
                params
            )

            return UtAssembleModel(addr, classId, modelName, instantiationCall)
        }

    companion object {
        private val runnableType: RefType
            get() = Scene.v().getSootClass(Runnable::class.qualifiedName).type!!
        private val targetFieldId: FieldId
            get() = utThreadClass.getField("target", runnableType).fieldId
    }
}

class ThreadGroupWrapper : BaseOverriddenWrapper(utThreadGroupClass.name) {
    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? = null

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel =
        resolver.run {
            val classId = THREAD_GROUP_TYPE.id
            val addr = holder.concreteAddr(wrapper.addr)
            val modelName = nextModelName("threadGroup")

            val values = collectFieldModels(wrapper.addr, overriddenClass.type)
            val nameModel = values[nameFieldId] ?: UtNullModel(stringClassId)

            val instantiationCall = UtExecutableCallModel(
                instance = null,
                constructorId(classId, stringClassId),
                listOf(nameModel)
            )

            return UtAssembleModel(addr, classId, modelName, instantiationCall)
        }

    companion object {
        private val nameFieldId: FieldId
            get() = utThreadGroupClass.getField("name", STRING_TYPE).fieldId
    }
}

private val TO_COMPLETABLE_FUTURE_SIGNATURE: String =
    utCompletableFutureClass.getMethodByName(UtCompletableFuture<*>::toCompletableFuture.name).signature
private val UT_COMPLETABLE_FUTURE_EQ_GENERIC_TYPE_SIGNATURE: String =
    utCompletableFutureClass.getMethodByName(UtCompletableFuture<*>::eqGenericType.name).signature

class CompletableFutureWrapper : BaseOverriddenWrapper(utCompletableFutureClass.name) {
    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? =
        when (method.signature) {
            TO_COMPLETABLE_FUTURE_SIGNATURE -> {
                val typeStorage = TypeStorage.constructTypeStorageWithSingleType(method.returnType)
                val resultingWrapper = wrapper.copy(typeStorage = typeStorage)
                val methodResult = MethodResult(resultingWrapper)

                listOf(methodResult)
            }
            UT_COMPLETABLE_FUTURE_EQ_GENERIC_TYPE_SIGNATURE -> {
                val firstParameter = parameters.single()
                val genericTypeParameterTypeConstraint = typeRegistry.typeConstraintToGenericTypeParameter(
                    firstParameter.addr,
                    wrapper.addr,
                    i = 0
                )
                val methodResult = MethodResult(
                    firstParameter,
                    genericTypeParameterTypeConstraint.asHardConstraint()
                )

                listOf(methodResult)
            }
            else -> null
        }

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel =
        resolver.run {
            val classId = COMPLETABLE_FUTURE_TYPE.id
            val addr = holder.concreteAddr(wrapper.addr)
            val modelName = nextModelName("completableFuture")

            val values = collectFieldModels(wrapper.addr, overriddenClass.type)
            val resultModel = values[resultFieldId] ?: UtNullModel(objectClassId)

            val instantiationCall = UtExecutableCallModel(
                instance = null,
                constructorId(classId, objectClassId),
                listOf(resultModel)
            )

            return UtAssembleModel(addr, classId, modelName, instantiationCall)
        }

    companion object {
        private val resultFieldId: FieldId
            get() = utCompletableFutureClass.getField("result", OBJECT_TYPE).fieldId
    }
}

class ExecutorServiceWrapper : BaseOverriddenWrapper(utExecutorServiceClass.name) {
    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? = null

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel =
        resolver.run {
            val classId = EXECUTORS_TYPE.id
            val addr = holder.concreteAddr(wrapper.addr)
            val modelName = nextModelName("executorService")

            val instantiationCall = UtExecutableCallModel(
                instance = null,
                newSingleThreadExecutorMethod,
                emptyList()
            )

            return UtAssembleModel(addr, classId, modelName, instantiationCall)
        }

    companion object {
        val newSingleThreadExecutorMethod: ExecutableId
            get() = EXECUTORS_TYPE.sootClass.getMethod("newSingleThreadExecutor", emptyList()).executableId
    }
}

class CountDownLatchWrapper : BaseOverriddenWrapper(utCountDownLatchClass.name) {
    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? = null

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel =
        resolver.run {
            val classId = COUNT_DOWN_LATCH_TYPE.id
            val addr = holder.concreteAddr(wrapper.addr)
            val modelName = nextModelName("countDownLatch")

            val values = collectFieldModels(wrapper.addr, overriddenClass.type)
            val countModel = values[countFieldId] ?: intClassId.defaultValueModel()

            val instantiationCall = UtExecutableCallModel(
                instance = null,
                constructorId(classId, intClassId),
                listOf(countModel)
            )

            return UtAssembleModel(addr, classId, modelName, instantiationCall)
        }

    companion object {
        private val countFieldId: FieldId
            get() = utCountDownLatchClass.getField("count", IntType.v()).fieldId
    }
}
