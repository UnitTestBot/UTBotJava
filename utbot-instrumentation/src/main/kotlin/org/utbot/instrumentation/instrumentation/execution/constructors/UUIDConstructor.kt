package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.*
import java.util.*

internal class UUIDConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as UUID

        return with(internalConstructor) {
            UtExecutableCallModel(
                instance = null,
                constructorId(classId, longClassId, longClassId),
                listOf(
                    construct(value.mostSignificantBits, longClassId),
                    construct(value.leastSignificantBits, longClassId),
                ),
            )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel> = emptyList()
}