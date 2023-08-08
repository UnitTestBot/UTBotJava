package org.utbot.fuzzing.spring.valid

import org.utbot.fuzzing.spring.FuzzedTypeFlag

object NotEmptyTypeFlag : FuzzedTypeFlag

class NotEmptyStringValueProvider : AbstractPrimitiveValidValueProvider<NotEmptyTypeFlag, Unit, String>() {
    override val primitiveClass: Class<String>
        get() = String::class.java

    override val validationDataTypeProperty get() = NotEmptyTypeFlag

    override fun defaultValidPrimitiveValue(validationData: Unit): String = " "

    override fun makeValid(originalValue: String, validationData: Unit): String =
        originalValue.ifEmpty { " " }

    override fun isNullValid(): Boolean = false
}