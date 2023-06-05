package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.primitiveByWrapper
import org.utbot.framework.plugin.api.util.stringClassId

internal class PrimitiveWrapperConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtStatementCallModel {
        checkClassCast(classId.jClass, value::class.java)

        return UtStatementCallModel(
            instance = null,
            constructorId(classId, classId.unbox()),
            listOf(UtPrimitiveModel(value))
        )
        
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel> = emptyList()
}


private fun ClassId.unbox() = if (this == stringClassId) {
    stringClassId
} else {
    primitiveByWrapper.getOrElse(this) { error("Unknown primitive wrapper: $this") }
}