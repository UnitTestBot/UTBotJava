package org.utbot.go.framework.api.go

/**
 * Parent class for all Go types.
 *
 * To see its children check GoTypesApi.kt at org.utbot.go.api.
 */
abstract class GoTypeId(
    val name: String,
    val elementTypeId: GoTypeId? = null,
    val implementsError: Boolean = false
) {
    open val sourcePackage: GoPackage = GoPackage("", "")
    abstract val canonicalName: String

    abstract fun getRelativeName(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): String
    override fun toString(): String = canonicalName
}

/**
 * Parent class for all Go models.
 *
 * To see its children check GoUtModelsApi.kt at org.utbot.go.api.
 */
abstract class GoUtModel(
    open val typeId: GoTypeId,
) {
    abstract fun getRequiredPackages(destinationPackage: GoPackage): Set<GoPackage>
    abstract fun isComparable(): Boolean
}

/**
 * Class for Go package.
 */
data class GoPackage(
    val name: String,
    val path: String
) {
    val isBuiltin = name == "" && path == ""
}

/**
 * Class for Go import.
 */
data class GoImport(
    val goPackage: GoPackage,
    val alias: String? = null
) {
    override fun toString(): String {
        if (alias == null) {
            return "\"${goPackage.path}\""
        }
        return "$alias \"${goPackage.path}\""
    }
}