package org.utbot.go.api

import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId

/**
 * Represents real Go primitive type.
 */
class GoPrimitiveTypeId(name: String) : GoTypeId(name) {
    override val canonicalName: String = when (name) {
        "byte" -> "uint8"
        "rune" -> "int32"
        else -> name
    }

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoPrimitiveTypeId) return false

        return canonicalName == other.canonicalName
    }

    override fun hashCode(): Int = name.hashCode()
}

/**
 * Class for Go struct field.
 */
data class GoFieldId(
    val declaringType: GoTypeId,
    val name: String,
    val isExported: Boolean
)

/**
 * Represents real Go struct type.
 */
class GoStructTypeId(
    name: String,
    var fields: List<GoFieldId>,
) : GoTypeId(name) {
    override val canonicalName: String = fields.joinToString(separator = ";", prefix = "struct{", postfix = "}") {
        "${it.name} ${it.declaringType}"
    }

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoStructTypeId) return false

        return fields == other.fields
    }

    override fun hashCode(): Int = fields.hashCode()
}

/**
 * Represents real Go array type.
 */
class GoArrayTypeId(
    name: String, elementTypeId: GoTypeId, val length: Int
) : GoTypeId(name, elementTypeId = elementTypeId) {
    override val canonicalName: String = "[$length]${elementTypeId}"

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String =
        "[$length]${elementTypeId!!.getRelativeName(destinationPackage, aliases)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoArrayTypeId) return false

        return elementTypeId == other.elementTypeId && length == other.length
    }

    override fun hashCode(): Int = 31 * elementTypeId.hashCode() + length
}

/**
 * Represents real Go slice type.
 */
class GoSliceTypeId(
    name: String, elementTypeId: GoTypeId,
) : GoTypeId(name, elementTypeId = elementTypeId) {
    override val canonicalName: String = "[]${elementTypeId}"

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String =
        "[]${elementTypeId!!.getRelativeName(destinationPackage, aliases)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoSliceTypeId) return false

        return elementTypeId == other.elementTypeId
    }

    override fun hashCode(): Int = elementTypeId.hashCode()
}

/**
 * Represents real Go map type.
 */
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

/**
 * Represents real Go chan type.
 */
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

    override val canonicalName: String = "$typeWithDirection $elementTypeId"

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

/**
 * Represents real Go interface type.
 */
class GoInterfaceTypeId(name: String, val implementations: List<GoTypeId>) : GoTypeId(name) {
    override val canonicalName: String = name

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoInterfaceTypeId) return false

        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

/**
 * Represents real Go named type.
 */
class GoNamedTypeId(
    name: String, override val sourcePackage: GoPackage, implementsError: Boolean, val underlyingTypeId: GoTypeId
) : GoTypeId(name, implementsError = implementsError) {
    private val packageName: String = sourcePackage.name
    private val packagePath: String = sourcePackage.path
    override val canonicalName: String = if (sourcePackage.isBuiltin) {
        name
    } else {
        "$packagePath/$packageName.$name"
    }

    fun exported(): Boolean = name.first().isUpperCase()

    override fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String {
        val alias = aliases[sourcePackage]
        return if (sourcePackage.isBuiltin || sourcePackage == destinationPackage || alias == ".") {
            name
        } else if (alias == null) {
            "${packageName}.${name}"
        } else {
            "${alias}.${name}"
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

/**
 * Represents real Go pointer type.
 */
class GoPointerTypeId(name: String, elementTypeId: GoTypeId) : GoTypeId(name, elementTypeId = elementTypeId) {
    override val canonicalName: String = "*$elementTypeId"

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
