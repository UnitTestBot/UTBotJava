package org.utbot.go.api

import org.utbot.go.framework.api.go.GoFieldId
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId

/**
 * Represents real Go primitive type.
 */
class GoPrimitiveTypeId(name: String) : GoTypeId(name) {
    override val canonicalName: String = when (name) {
        "byte" -> "uint8"
        "rune" -> "int32"
        else -> simpleName
    }

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String = simpleName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoPrimitiveTypeId) return false

        return canonicalName == other.canonicalName
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

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String {
        val alias = aliases[sourcePackage]
        return if (sourcePackage == destinationPackage || alias == ".") {
            simpleName
        } else if (alias == null) {
            "${packageName}.${simpleName}"
        } else {
            "${alias}.${simpleName}"
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

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String =
        "[$length]${elementTypeId!!.getRelativeName(destinationPackage, aliases)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoArrayTypeId) return false

        return elementTypeId == other.elementTypeId && length == other.length
    }

    override fun hashCode(): Int = 31 * elementTypeId.hashCode() + length
}

class GoSliceTypeId(
    name: String, elementTypeId: GoTypeId,
) : GoTypeId(name, elementTypeId = elementTypeId) {
    override val canonicalName: String = "[]${elementTypeId.canonicalName}"

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String =
        "[]${elementTypeId!!.getRelativeName(destinationPackage, aliases)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoArrayTypeId) return false

        return elementTypeId == other.elementTypeId
    }

    override fun hashCode(): Int = elementTypeId.hashCode()
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

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String {
        val alias = aliases[sourcePackage]
        return if (sourcePackage == destinationPackage || alias == ".") {
            simpleName
        } else if (alias == null) {
            "${packageName}.${simpleName}"
        } else {
            "${alias}.${simpleName}"
        }
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
