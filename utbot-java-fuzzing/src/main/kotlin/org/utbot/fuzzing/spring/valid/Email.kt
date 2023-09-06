package org.utbot.fuzzing.spring.valid

import org.utbot.fuzzing.spring.FuzzedTypeFlag

object EmailTypeFlag : FuzzedTypeFlag

class EmailValueProvider : AbstractPrimitiveValidValueProvider<EmailTypeFlag, Unit, String>() {
    override val primitiveClass: Class<String>
        get() = String::class.java

    override val validationDataTypeProperty get() = EmailTypeFlag

    override fun defaultValidPrimitiveValue(validationData: Unit): String = "johnDoe@sample.com"

    override fun makeValid(originalValue: String, validationData: Unit): String =
        when {
            originalValue.length < 2 -> "johnDoe@sample.com"
            else -> originalValue.take(originalValue.length / 2) + "@" + originalValue.drop(originalValue.length / 2)
        }

    // `null` is a valid email according to `jakarta.validation.constraints.Email` javadoc
    override fun isNullValid(): Boolean = true
}