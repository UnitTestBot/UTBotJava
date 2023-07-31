package org.utbot.instrumentation.instrumentation.execution.constructors

import java.util.stream.BaseStream
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementModel

internal fun findStreamConstructor(stream: BaseStream<*, *>): UtAssembleModelConstructorBase =
    when (stream) {
        is IntStream -> IntStreamConstructor()
        is LongStream -> LongStreamConstructor()
        is DoubleStream -> DoubleStreamConstructor()
        else -> BaseStreamConstructor()
    }

internal abstract class UtAssembleModelConstructorBase : UtModelWithCompositeOriginConstructor {
    override fun constructModelWithCompositeOrigin(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        valueClassId: ClassId,
        id: Int?,
        saveToCache: (UtModel) -> Unit
    ): UtAssembleModel {
        val baseName = valueClassId.simpleName.decapitalize()
        val instantiationCall = provideInstantiationCall(internalConstructor, value, valueClassId)
        return UtAssembleModel(id, valueClassId, nextModelName(baseName), instantiationCall) {
            saveToCache(this)
            provideModificationChain(internalConstructor, value)
        }
    }

    protected abstract fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtExecutableCallModel

    protected abstract fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel>
}

internal fun UtAssembleModelConstructorBase.checkClassCast(expected: Class<*>, actual: Class<*>) {
    require(expected.isAssignableFrom(actual)) {
        "Can't cast $actual to $expected in $this assemble constructor."
    }
}