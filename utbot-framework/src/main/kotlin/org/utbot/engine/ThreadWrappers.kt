package org.utbot.engine

import org.utbot.engine.overrides.threads.UtThread
import org.utbot.engine.overrides.threads.UtThreadGroup
import org.utbot.engine.types.STRING_TYPE
import org.utbot.engine.types.THREAD_GROUP_TYPE
import org.utbot.engine.types.THREAD_TYPE
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.util.nextModelName
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.SootField
import soot.SootMethod

val utThreadClass: SootClass
    get() = Scene.v().getSootClass(UtThread::class.qualifiedName)
val utThreadGroupClass: SootClass
    get() = Scene.v().getSootClass(UtThreadGroup::class.qualifiedName)

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

            val targetField = targetField.fieldId
            val values = collectFieldModels(wrapper.addr, overriddenClass.type)
            val targetModel = values[targetField] as? UtLambdaModel

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

//            val targetFieldChunkId = resolver.hierarchy.chunkIdForField(utThreadClass.type, targetField)
//            val targetFieldChunkDescriptor = MemoryChunkDescriptor(targetFieldChunkId, wrapper.type, runnableType)
//            val targetAddr = resolver.findArray(targetFieldChunkDescriptor, state).select(wrapper.addr)
        }

    companion object {
        private val runnableType: RefType = Scene.v().getSootClass(Runnable::class.qualifiedName).type!!
        private val targetField: SootField
            get() = utThreadClass.getField("target", runnableType)
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

            val nameField = nameField.fieldId
            val values = collectFieldModels(wrapper.addr, overriddenClass.type)
            val nameModel = values[nameField] ?: UtNullModel(stringClassId)

            val instantiationCall = UtExecutableCallModel(
                instance = null,
                constructorId(classId, stringClassId),
                listOf(nameModel)
            )

            return UtAssembleModel(addr, classId, modelName, instantiationCall)
        }

    companion object {
        private val nameField: SootField
            get() = utThreadGroupClass.getField("name", STRING_TYPE)
    }
}
