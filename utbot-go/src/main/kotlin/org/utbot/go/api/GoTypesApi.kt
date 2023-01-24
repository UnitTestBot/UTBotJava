package org.utbot.go.api

import org.utbot.go.framework.api.go.GoFieldId
import org.utbot.go.framework.api.go.GoTypeId

/**
 * Represents real Go primitive type.
 */
class GoPrimitiveTypeId(name: String) : GoTypeId(name) {
    override val canonicalName: String = simpleName

    override fun getRelativeName(packageName: String): String = simpleName
}

class GoStructTypeId(
    name: String,
    implementsError: Boolean,
    override val packageName: String,
    val packagePath: String,
    val fields: List<GoFieldId>,
) : GoTypeId(name, implementsError = implementsError) {
    override val canonicalName: String = "$packageName.$name"

    override fun getRelativeName(packageName: String): String =
        if (this.packageName != packageName) {
            canonicalName
        } else {
            simpleName
        }
}

class GoArrayTypeId(
    name: String,
    elementTypeId: GoTypeId,
    val length: Int
) : GoTypeId(name, elementClassId = elementTypeId) {
    override val canonicalName: String = "[$length]${elementTypeId.canonicalName}"
    val elementTypeId: GoTypeId = elementClassId as GoTypeId

    override fun getRelativeName(packageName: String): String = "[$length]${elementTypeId.getRelativeName(packageName)}"
}

class GoInterfaceTypeId(
    name: String,
    implementsError: Boolean
) : GoTypeId(name, implementsError = implementsError) {
    override fun getRelativeName(packageName: String): String {
        TODO("Not yet implemented")
    }
}
