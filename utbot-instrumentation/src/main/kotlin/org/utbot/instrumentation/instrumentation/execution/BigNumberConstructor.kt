package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.stringClassId

internal class BigNumberConstructor : UtAssembleModelConstructorBase() {

    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)

        val stringValue = value.toString()
        val stringValueModel = internalConstructor.construct(stringValue, stringClassId)

        return UtExecutableCallModel(
            instance = null,
            ConstructorId(classId, listOf(stringClassId)),
            listOf(stringValueModel),

            )
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel> = emptyList()
}
