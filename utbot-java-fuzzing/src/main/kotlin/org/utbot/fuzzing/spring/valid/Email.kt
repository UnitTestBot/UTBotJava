package org.utbot.fuzzing.spring.valid

import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.spring.FuzzedTypeFlag

object EmailTypeFlag : FuzzedTypeFlag

class EmailValueProvider : AbstractValidValueProvider<EmailTypeFlag, Unit>() {

    companion object {
        private const val SAMPLE_EMAIL = "johnDoe@sample.com"
    }

    override val validationDataTypeProperty get() = EmailTypeFlag

    override fun acceptsType(type: FuzzedType) = type.classId == stringClassId

    override fun makeValid(originalValue: FuzzedValue, validationData: Unit): FuzzedValue =
        when (val model = originalValue.model) {
             is UtNullModel -> originalValue // `null` is a valid email according to `jakarta.validation.constraints.Email` javadoc
             is UtPrimitiveModel -> (model.value as? String)?.let { str ->
                 if (str.contains("@")) originalValue
                 else {
                     val validEmailStr =
                         if (str.isEmpty()) SAMPLE_EMAIL
                         else str.take(str.length / 2) + "@" + str.drop(str.length / 2)
                     UtPrimitiveModel(validEmailStr).fuzzed {
                         summary = "%var% = \"$validEmailStr\""
                     }
                 }
             } ?: originalValue
             else -> originalValue
         }

    override fun defaultValidValue(validationData: Unit) = UtPrimitiveModel(SAMPLE_EMAIL).fuzzed {
        summary = "%var% = \"$SAMPLE_EMAIL\""
    }
}