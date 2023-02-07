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
    open val packageName: String = ""
    val simpleName: String = name
    abstract val canonicalName: String

    abstract fun getRelativeName(packageName: String): String
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
    open fun getRequiredImports(): Set<String> = emptySet()
    abstract fun isComparable(): Boolean
}

/**
 * Class for Go struct field model.
 */
class GoUtFieldModel(
    val model: GoUtModel,
    val fieldId: GoFieldId,
) : GoUtModel(fieldId.declaringType) {
    override fun getRequiredImports(): Set<String> = model.getRequiredImports()
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
