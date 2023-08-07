package org.utbot.fuzzing.spring.valid

import org.utbot.fuzzing.spring.FuzzedTypeFlag

object NotBlankTypeFlag : FuzzedTypeFlag

class NotBlankStringValueProvider : AbstractPrimitiveValidValueProvider<NotBlankTypeFlag, Unit, String>() {
    override val primitiveClass: Class<String>
        get() = String::class.java

    override val validationDataTypeProperty get() = NotBlankTypeFlag

    override fun defaultValidPrimitiveValue(validationData: Unit): String = "e"

    override fun makeValid(originalValue: String, validationData: Unit): String =
        originalValue.ifBlank { "e" }

    override fun isNullValid(): Boolean = false
}