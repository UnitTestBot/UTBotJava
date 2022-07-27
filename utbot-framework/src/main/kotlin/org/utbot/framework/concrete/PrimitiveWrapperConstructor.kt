package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.jcdb.api.unboxIfNeeded

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
            constructorId(classId, classId.unboxIfNeeded()),
            listOf(UtPrimitiveModel(valueToConstructFrom)),
            this
        )
    }
}