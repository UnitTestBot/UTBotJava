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
    override fun UtAssembleModel.modifyChains(
        internalConstructor: UtModelConstructorInterface,
        instantiationChain: MutableList<UtStatementModel>,
        modificationChain: MutableList<UtStatementModel>,
        valueToConstructFrom: Any
    ) {
        checkClassCast(classId.jClass, valueToConstructFrom::class.java)

        instantiationChain += UtExecutableCallModel(
            null,
            constructorId(classId, classId.unbox()),
            listOf(UtPrimitiveModel(valueToConstructFrom)),
            this
        )
    }
}


private fun ClassId.unbox() = if (this == stringClassId) {
    stringClassId
} else {
    primitiveByWrapper.getOrElse(this) { error("Unknown primitive wrapper: $this") }
}