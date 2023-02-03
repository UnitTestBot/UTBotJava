package org.utbot.go.framework.api.go

import org.utbot.framework.plugin.api.*

/**
 * Parent class for all Go types for compatibility with UTBot framework.
 *
 * To see its children check GoTypesApi.kt at org.utbot.go.api.
 */
abstract class GoTypeId(
    name: String, elementClassId: GoTypeId? = null, val implementsError: Boolean = false
) : ClassId(name, elementClassId) {
    override val isNullable: Boolean
        get() = error("not supported")
    override val canonicalName: String
        get() = error("not supported")
    override val simpleName: String
        get() = name
    override val packageName: String
        get() = error("not supported")
    override val isInDefaultPackage: Boolean
        get() = error("not supported")
    override val isAnonymous: Boolean
        get() = error("not supported")
    override val isLocal: Boolean
        get() = error("not supported")
    override val isInner: Boolean
        get() = error("not supported")
    override val isNested: Boolean
        get() = error("not supported")
    override val isSynthetic: Boolean
        get() = error("not supported")
    override val allMethods: Sequence<MethodId>
        get() = error("not supported")
    override val allConstructors: Sequence<ConstructorId>
        get() = error("not supported")
    override val typeParameters: TypeParameters
        get() = error("not supported")
    override val outerClass: Class<*>?
        get() = error("not supported")

    abstract fun getRelativeName(packageName: String): String
}

/**
 * Parent class for all Go models.
 *
 * To see its children check GoUtModelsApi.kt at org.utbot.go.api.
 */
abstract class GoUtModel(
    override val classId: GoTypeId,
) : UtModel(classId) {
    open fun getRequiredImports(): Set<String> = emptySet()
    abstract fun isComparable(): Boolean
    override fun toString(): String = error("not supported")
}

/**
 * Class for Go struct field model.
 */
class GoUtFieldModel(
    val model: GoUtModel,
    val fieldId: GoFieldId,
) : UtModel(fieldId.declaringClass)

/**
 * Class for Go struct field.
 */
class GoFieldId(
    declaringClass: GoTypeId, name: String, val isExported: Boolean
) : FieldId(declaringClass, name) {
    override fun toString(): String = "$name: $declaringClass"
}
