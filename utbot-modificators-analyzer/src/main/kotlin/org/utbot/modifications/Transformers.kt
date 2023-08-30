package org.utbot.modifications

import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.fieldId
import soot.Unit
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInstanceFieldRef

/**
 * Describes the type of field involvement meaningful for the current analysis type.
 * For example, we can be interested on modifications only or in read attempts too.
 *
 * @param projector shows the rule to collect a set of [FieldId]s from the statements
 */
enum class FieldInvolvementMode(val projector: (Unit) -> FieldId?) {
    WriteOnly(
        {
            when (it) {
                is JAssignStmt -> {
                    (it.leftOp as? JInstanceFieldRef)?.field?.fieldId
                }

                else -> null
            }
        }
    ),

    ReadAndWrite(
        {
            when (it) {
                is JAssignStmt -> {
                    (it.leftOp as? JInstanceFieldRef)?.field?.fieldId
                        ?: (it.rightOp as? JInstanceFieldRef)?.field?.fieldId
                }

                else -> null
            }
        }
    ),
}
