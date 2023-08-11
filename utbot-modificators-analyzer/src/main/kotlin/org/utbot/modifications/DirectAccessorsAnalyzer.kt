package org.utbot.modifications

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.DirectFieldAccessId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.jClass
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Analyzer of direct accessors to public fields.
 */
class DirectAccessorsAnalyzer {
    /**
     * Collect all direct accesses to fields in classes.
     */
    fun collectDirectAccesses(classIds: Set<ClassId>): Set<DirectFieldAccessId> =
        classIds
            .flatMap { classId -> collectFields(classId) }
            .map { fieldId -> DirectFieldAccessId(fieldId.declaringClass, directSetterName(fieldId), fieldId) }
            .toSet()

    /**
     * Collect all fields with different non-private modifiers
     * from class [classId] or it's base classes.
     */
    private fun collectFields(classId: ClassId): Set<FieldId> {
        var clazz = classId.jClass

        val fieldIds = mutableSetOf<Field>()
        fieldIds += clazz.declaredFields.filterNot { Modifier.isPrivate(it.modifiers) }
        while (clazz.superclass != null) {
            clazz = clazz.superclass
            fieldIds += clazz.declaredFields.filterNot { Modifier.isPrivate(it.modifiers) }
        }


        return fieldIds.map { it.fieldId }.toSet()
    }

    /**
     * Creates a name of direct value setting to field.
     */
    private fun directSetterName(field: FieldId) = "direct_set_${field.name}"
}
