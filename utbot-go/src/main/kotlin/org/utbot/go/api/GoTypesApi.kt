package org.utbot.go.api

import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.go.framework.api.go.GoStructConstructorId
import org.utbot.go.framework.api.go.GoTypeId

/**
 * Represents real Go primitive type.
 */
class GoPrimitiveTypeId(name: String) : GoTypeId(name) {
    override val canonicalName: String = simpleName

    override fun getRelativeName(packageName: String): String = simpleName
}

class GoFieldId(
    declaringClass: GoTypeId,
    name: String,
    val isExported: Boolean
) : FieldId(declaringClass, name)

class GoStructTypeId(
    name: String,
    implementsError: Boolean,
    override val packageName: String,
    val packagePath: String,
    val fields: List<GoFieldId>,
) : GoTypeId(name, implementsError = implementsError) {
    override val canonicalName: String = "$packageName.$name"

    override val allConstructors: Sequence<ConstructorId>
        get() = sequenceOf(GoStructConstructorId(this, fields))

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

    override fun getRelativeName(packageName: String): String = "[$length]${elementTypeId.getRelativeName(packageName)}"

    val elementTypeId: GoTypeId = elementClassId as GoTypeId
}

class GoInterfaceTypeId(
    name: String,
    implementsError: Boolean
) : GoTypeId(name, implementsError = implementsError) {
    override fun getRelativeName(packageName: String): String {
        TODO("Not yet implemented")
    }
}
