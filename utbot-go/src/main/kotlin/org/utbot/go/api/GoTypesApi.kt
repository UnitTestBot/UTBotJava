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
    val fields: List<GoFieldId>,
) : GoTypeId(name) {
    override val canonicalName: String = name

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String = simpleName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoStructTypeId) return false

        return fields == other.fields
    }

    override fun hashCode(): Int {
        return fields.hashCode()
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
        if (other !is GoSliceTypeId) return false

        return elementTypeId == other.elementTypeId
    }

    override fun hashCode(): Int = elementTypeId.hashCode()
}

class GoInterfaceTypeId(name: String) : GoTypeId(name) {
    override val canonicalName: String = name

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String = simpleName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoInterfaceTypeId) return false

        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

class GoNamedTypeId(
    name: String,
    override val sourcePackage: GoPackage,
    implementsError: Boolean,
    val underlyingTypeId: GoTypeId
) : GoTypeId(name, implementsError = implementsError) {
    val packageName: String = sourcePackage.packageName
    val packagePath: String = sourcePackage.packagePath
    override val canonicalName: String = if (sourcePackage.packageName != "") {
        "${sourcePackage.packageName}.$name"
    } else {
        name
    }

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String {
        val alias = aliases[sourcePackage]
        return if (sourcePackage.isBuiltin || sourcePackage == destinationPackage || alias == ".") {
            simpleName
        } else if (alias == null) {
            "${packageName}.${simpleName}"
        } else {
            "${alias}.${simpleName}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoNamedTypeId) return false

        return sourcePackage == other.sourcePackage && name == other.name
    }

    override fun hashCode(): Int {
        var result = packagePath.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}
