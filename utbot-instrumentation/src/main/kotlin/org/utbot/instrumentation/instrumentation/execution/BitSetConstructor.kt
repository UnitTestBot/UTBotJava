package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.longArrayClassId
import org.utbot.framework.plugin.api.util.methodId

internal class BitSetConstructor : UtAssembleModelConstructorBase() {

    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.util.BitSet
        return with(internalConstructor) {
            UtExecutableCallModel(
                instance = null,
                methodId(classId, "valueOf", classId, longArrayClassId),
                listOf(
                    construct(value.toLongArray(), longArrayClassId),
                ),
            )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel> = emptyList()

}
