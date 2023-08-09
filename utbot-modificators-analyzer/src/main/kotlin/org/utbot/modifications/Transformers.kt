package org.utbot.modifications

import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.fieldId
import soot.Unit
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInstanceFieldRef

enum class ModificationTransformationMode(val transformer: (Unit) -> FieldId?) {
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
