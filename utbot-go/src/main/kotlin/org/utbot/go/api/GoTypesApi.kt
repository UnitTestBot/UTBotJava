package org.utbot.go.api

import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.go.framework.api.go.GoClassId
import org.utbot.go.framework.api.go.GoStructConstructorId

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
    override val packageName: String,
    val packagePath: String,
    implementsError: Boolean,
    val fields: List<FieldId>,
) : GoTypeId(name, implementsError) {
    override val allConstructors: Sequence<ConstructorId>
        get() = sequenceOf(GoStructConstructorId(this, fields))

    fun getNameRelativeToPackage(packageName: String): String = if (this.packageName != packageName) {
        this.packageName + "."
    } else {
        ""
    } + simpleName
}

class GoArrayTypeId(
    name: String,
    elementTypeId: GoTypeId,
    val length: Int
) : GoTypeId(name, elementClassId = elementTypeId) {
    val elementTypeId: GoTypeId = elementClassId as GoTypeId
}

class GoInterfaceTypeId(
    name: String,
    implementsError: Boolean
) : GoTypeId(name, implementsError)
