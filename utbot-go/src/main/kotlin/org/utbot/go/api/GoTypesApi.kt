package org.utbot.go.api

import org.utbot.go.framework.api.go.GoFieldId
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId

/**
 * Represents real Go primitive type.
 */
class GoPrimitiveTypeId(name: String) : GoTypeId(name) {
    override val canonicalName: String = simpleName

    override fun getRelativeName(goPackage: GoPackage, alias: String): String = simpleName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoPrimitiveTypeId) return false

        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

class GoStructTypeId(
    name: String,
    implementsError: Boolean,
    override val sourcePackage: GoPackage,
    val fields: List<GoFieldId>,
) : GoTypeId(name, implementsError = implementsError) {
    val packageName: String = sourcePackage.packageName
    val packagePath: String = sourcePackage.packagePath
    override val canonicalName: String = "${sourcePackage.packageName}.$name"

    override fun getRelativeName(goPackage: GoPackage, alias: String): String = if (alias == "") {
        if (sourcePackage == goPackage) {
            simpleName
        } else {
            "$packageName.$simpleName"
        }
    } else {
        when (alias) {
            "." -> simpleName
            "_" -> error("not supported")
            else -> "$alias.$simpleName"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoStructTypeId) return false

        return packagePath == other.packagePath && packageName == other.packageName && name == other.name
    }

    override fun hashCode(): Int {
        var result = packagePath.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

class GoArrayTypeId(
    name: String, elementTypeId: GoTypeId, val length: Int
) : GoTypeId(name, elementTypeId = elementTypeId) {
    override val canonicalName: String = "[$length]${elementTypeId.canonicalName}"

    override fun getRelativeName(goPackage: GoPackage, alias: String): String =
        "[$length]${elementTypeId!!.getRelativeName(goPackage, alias)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoArrayTypeId) return false

        return elementTypeId == other.elementTypeId && length == other.length
    }

    override fun hashCode(): Int = 31 * elementTypeId.hashCode() + length
}

class GoInterfaceTypeId(
    name: String,
    implementsError: Boolean,
    override val sourcePackage: GoPackage,
) : GoTypeId(name, implementsError = implementsError) {
    val packageName: String = sourcePackage.packageName
    val packagePath: String = sourcePackage.packagePath
    override val canonicalName: String = if (packageName != "") {
        "$packageName.$name"
    } else {
        simpleName
    }

    override fun getRelativeName(goPackage: GoPackage, alias: String): String = if (this.packageName != packageName) {
        canonicalName
    } else {
        simpleName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoInterfaceTypeId) return false

        return packagePath == other.packagePath && packageName == other.packageName && name == other.name
    }

    override fun hashCode(): Int {
        var result = packagePath.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}
