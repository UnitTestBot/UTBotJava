package org.utbot.fuzzing.spring.valid

import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.spring.FuzzedTypeFlag

object NotEmptyTypeFlag : FuzzedTypeFlag

class NotEmptyStringValueProvider : AbstractValidValueProvider<NotEmptyTypeFlag, Unit>() {
    override val validationDataTypeProperty get() = NotEmptyTypeFlag

    override fun acceptsType(type: FuzzedType): Boolean = type.classId == stringClassId

    override fun makeValid(originalValue: FuzzedValue, validationData: Unit): FuzzedValue =
        when (val model = originalValue.model) {
            is UtNullModel -> defaultValidValue(validationData)
            is UtPrimitiveModel -> {
                (model.value as? String)?.let {
                    if (it.isEmpty()) defaultValidValue(validationData)
                    else originalValue
                } ?: originalValue
            }

            else -> originalValue
        }

    override fun defaultValidValue(validationData: Unit) = UtPrimitiveModel(" ").fuzzed {
        summary = "%var% = \" \""
    }
}