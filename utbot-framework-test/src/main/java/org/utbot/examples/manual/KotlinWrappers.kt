package org.utbot.examples.manual

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel

fun fields(
    classId: ClassId,
    vararg fields: Pair<String, Any>
): MutableMap<FieldId, UtModel> {
    return fields
        .associate {
            val fieldId = FieldId(classId, it.first)
            val fieldValue = when (val value = it.second) {
                is UtModel -> value
                else -> UtPrimitiveModel(value)
            }
            fieldId to fieldValue
        }
        .toMutableMap()
}