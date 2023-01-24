package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.stringClassId
import java.util.*

internal class LocaleConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as Locale

        return with(internalConstructor) {
            UtExecutableCallModel(
                instance = null,
                methodId(classId, "forLanguageTag", classId, stringClassId),
                listOf(
                    construct(value.toLanguageTag(), stringClassId),
                ),
            )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel> = emptyList()
}