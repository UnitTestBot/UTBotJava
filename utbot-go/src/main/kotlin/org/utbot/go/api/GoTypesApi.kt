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
    var fields: List<GoFieldId>,
) : GoTypeId(name) {
    override val canonicalName: String = name

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String = simpleName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoStructTypeId) return false

        return fields == other.fields
    }

    override fun hashCode(): Int = fields.hashCode()
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

class GoMapTypeId(
    name: String, val keyTypeId: GoTypeId, elementTypeId: GoTypeId,
) : GoTypeId(name, elementTypeId = elementTypeId) {
    override val canonicalName: String = "map[${keyTypeId.canonicalName}]${elementTypeId.canonicalName}"

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String {
        val keyType = keyTypeId.getRelativeName(destinationPackage, aliases)
        val elementType = elementTypeId!!.getRelativeName(destinationPackage, aliases)
        return "map[$keyType]$elementType"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoMapTypeId) return false

        return keyTypeId == other.keyTypeId && elementTypeId == other.elementTypeId
    }

    override fun hashCode(): Int = 31 * keyTypeId.hashCode() + elementTypeId.hashCode()
}

class GoChanTypeId(
    name: String, elementTypeId: GoTypeId, val direction: Direction,
) : GoTypeId(name, elementTypeId = elementTypeId) {
    enum class Direction {
        SENDONLY, RECVONLY, SENDRECV
    }

    private val typeWithDirection = when (direction) {
        Direction.RECVONLY -> "<-chan"
        Direction.SENDONLY -> "chan<-"
        Direction.SENDRECV -> "chan"
    }

    override val canonicalName: String = "$typeWithDirection ${elementTypeId.canonicalName}"

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String {
        val elementType = elementTypeId!!.getRelativeName(destinationPackage, aliases)
        return "$typeWithDirection $elementType"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoChanTypeId) return false

        return elementTypeId == other.elementTypeId && direction == other.direction
    }

    override fun hashCode(): Int = 31 * elementTypeId.hashCode() + direction.hashCode()
}

class GoInterfaceTypeId(name: String, val implementations: List<GoTypeId>) : GoTypeId(name) {
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
    name: String, override val sourcePackage: GoPackage, implementsError: Boolean, val underlyingTypeId: GoTypeId
) : GoTypeId(name, implementsError = implementsError) {
    val packageName: String = sourcePackage.packageName
    val packagePath: String = sourcePackage.packagePath
    override val canonicalName: String = if (sourcePackage.isBuiltin) {
        name
    } else {
        "${sourcePackage.packageName}.$name"
    }

    fun exported(): Boolean = name.first().isUpperCase()

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

class GoPointerTypeId(name: String, elementTypeId: GoTypeId) : GoTypeId(name, elementTypeId = elementTypeId) {
    override val canonicalName: String = if (sourcePackage.isBuiltin) {
        name
    } else {
        "${sourcePackage.packageName}.$name"
    }

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String {
        val elementType = elementTypeId!!.getRelativeName(destinationPackage, aliases)
        return "*$elementType"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoPointerTypeId) return false

        return elementTypeId == other.elementTypeId
    }

    override fun hashCode(): Int = elementTypeId.hashCode()
}
