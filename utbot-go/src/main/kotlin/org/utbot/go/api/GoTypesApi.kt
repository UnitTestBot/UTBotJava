package org.utbot.go.api

import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.framework.api.go.GoClassId
import org.utbot.go.framework.api.go.GoStructConstructorId
import org.utbot.go.framework.api.go.GoUtModel

/**
 * Represents real Go type.
 *
 * Note that unique identifier of GoTypeId (as for any children of GoClassId) is its name.
 */
open class GoTypeId(
    name: String,
    val implementsError: Boolean = false,
    elementClassId: GoClassId? = null
) : GoClassId(name, elementClassId) {
    override val simpleName: String
        get() = name
}

class GoStructTypeId(
    name: String,
    implementsError: Boolean,
    val fields: List<FieldId>,
) : GoTypeId(name, implementsError) {
    override val allConstructors: Sequence<ConstructorId>
        get() = sequenceOf(GoStructConstructorId(this, fields))
}

class GoArrayTypeId(
    name: String,
    elementTypeId: GoTypeId,
    val length: Int
) : GoTypeId(name, elementClassId = elementTypeId) {
    val elementTypeId: GoTypeId = elementClassId as GoTypeId

    fun getDefaultValueModelForElement(): GoUtModel = elementTypeId.goDefaultValueModel()
}

class GoInterfaceTypeId(
    name: String,
    implementsError: Boolean
): GoTypeId(name, implementsError)

// Wraps tuple of several types into one GoClassId. It helps to handle multiple result types of Go functions.
class GoSyntheticMultipleTypesId(val types: List<GoTypeId>) :
    GoClassId("synthetic_multiple_types${types.typesToString()}") {
    override fun toString(): String = types.typesToString()
}

private fun List<GoTypeId>.typesToString(): String = this.joinToString(separator = ", ", prefix = "(", postfix = ")")

// There is no void type in Go; therefore, this class solves function returns nothing case.
class GoSyntheticNoTypeId : GoClassId("")