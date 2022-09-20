package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
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
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)

        return UtExecutableCallModel(
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