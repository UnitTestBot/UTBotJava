package org.utbot.go.framework.api.go

/**
 * Parent class for all Go types for compatibility with UTBot framework.
 *
 * To see its children check GoTypesApi.kt at org.utbot.go.api.
 */
abstract class GoTypeId(
    val name: String,
    val elementTypeId: GoTypeId? = null,
    val implementsError: Boolean = false
) {
    open val sourcePackage: GoPackage = GoPackage("", "")
    val simpleName: String = name
    abstract val canonicalName: String

    abstract fun getRelativeName(goPackage: GoPackage, alias: String = ""): String
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
    open fun getRequiredPackages(): Set<GoPackage> = emptySet()
    abstract fun isComparable(): Boolean
}

/**
 * Class for Go struct field model.
 */
class GoUtFieldModel(
    val model: GoUtModel,
    val fieldId: GoFieldId,
) : GoUtModel(fieldId.declaringType) {
    override fun getRequiredPackages(): Set<GoPackage> = model.getRequiredPackages()
    override fun isComparable(): Boolean = model.isComparable()
}

/**
 * Class for Go struct field.
 */
class GoFieldId(
    val declaringType: GoTypeId,
    val name: String,
    val isExported: Boolean
)

/**
 * Class for Go package.
 */
data class GoPackage(
    val packageName: String,
    val packagePath: String
)

/**
 * Class for Go import.
 */
data class GoImport(
    val goPackage: GoPackage,
    val alias: String
) {
    override fun toString(): String {
        if (alias == "") {
            return "\"${goPackage.packagePath}\""
        }
        return "$alias \"${goPackage.packagePath}\""
    }
}