package org.utbot.framework.fields

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtModel

data class StateModificationInfo(
        val thisInstance: ModifiedFields = emptyList(),
        val parameters: List<ModifiedFields> = emptyList(),
        val staticFields: Map<ClassId, ModifiedFields> = emptyMap()
)

data class FieldStatesInfo(
        val fieldsBefore: Map<FieldPath, UtModel>,
        val fieldsAfter: Map<FieldPath, UtModel>
) {
    /**
     * By design the 'before' and 'after' states contain info about the same fields.
     * It means that it is not possible for a field to be present at 'before' and to be absent at 'after'.
     * The reverse is also impossible.
     */
    val fields: Set<FieldPath>
        get() = fieldsBefore.keys
}

sealed class FieldPathElement {
    abstract val type: ClassId
}

data class FieldAccess(val field: FieldId) : FieldPathElement() {
    override val type: ClassId
        get() = this.field.type
}

data class ArrayElementAccess(
        override val type: ClassId,
        val index: Int
) : FieldPathElement()

data class FieldPath(val elements: List<FieldPathElement> = mutableListOf()) {
    operator fun plus(element: FieldPathElement): FieldPath {
        return FieldPath(elements + element)
    }

    val fieldType: ClassId
        get() = elements.last().type

    override fun toString(): String {
        return elements.joinToString(separator = "") {
            when (it) {
                is FieldAccess -> ".${it.field.name}"
                is ArrayElementAccess -> "[${it.index}]"
            }
        }
    }
}

typealias ModifiedFields = List<ModifiedField>

data class ModifiedField(val path: FieldPath, val before: UtModel, val after: UtModel)
