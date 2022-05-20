package org.utbot.framework.modifications

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId

/**
 * Information about the statement in the invocation graph
 * @param isRoot Is it a root for invocation graph building
 * @param declaringClass Class where statement is declared
 * @param modifiedFields Fields modified in current statement
 * @param successors Statements called from current statement
 * @param filledVersion Stamp of the last information update
 * @param traverseOrderStamp Stamp of traverse during invocation graph building (for Kosaraju algorithm)
 * @param allModifiedFields Fields modified in current statement and in nested calls
 * @param componentId Component id in invocation graph (for Kosaraju algorithm)
 */
data class StatementInfo(
    val isRoot: Boolean,
    val declaringClass: ClassId,
    val modifiedFields: Set<FieldId>,
    val successors: Set<ExecutableId>,
    val filledVersion: Long,
    val traverseOrderStamp: Long = -1,
    val allModifiedFields: Set<FieldId> = mutableSetOf(),
    var componentId: Int = -1
)

/**
 * Updates method state with new transient modified fields.
 */
fun StatementInfo.appendModifiedFields(newFields: Set<FieldId>): StatementInfo =
    this.copy(allModifiedFields = allModifiedFields + newFields)

