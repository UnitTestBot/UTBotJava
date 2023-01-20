package org.utbot.python.framework.codegen.model.services.access

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.access.CgFieldStateManager
import org.utbot.framework.codegen.services.language.JavaCgLanguageAssistant.getCallableAccessManagerBy
import org.utbot.framework.codegen.services.language.JavaCgLanguageAssistant.getStatementConstructorBy
import org.utbot.framework.codegen.tree.CgFieldState
import org.utbot.framework.codegen.tree.CgStatementConstructor
import org.utbot.framework.codegen.tree.FieldStateCache
import org.utbot.framework.fields.ArrayElementAccess
import org.utbot.framework.fields.FieldAccess
import org.utbot.framework.fields.FieldPath
import org.utbot.framework.fields.ModifiedField
import org.utbot.framework.fields.StateModificationInfo
import org.utbot.framework.plugin.api.util.hasField
import org.utbot.framework.plugin.api.util.isArray

class PythonCgFieldStateManager(val context: CgContext) :
    CgContextOwner by context,
    CgFieldStateManager,
    CgCallableAccessManager by getCallableAccessManagerBy(context),
    CgStatementConstructor by getStatementConstructorBy(context) {

    private enum class FieldState(val variablePrefix: String) {
        INITIAL("initial"),
        FINAL("final")
    }

    override fun rememberInitialEnvironmentState(info: StateModificationInfo) {
        rememberThisInstanceState(info, FieldState.INITIAL)
        rememberArgumentsState(info, FieldState.INITIAL)
    }

    override fun rememberFinalEnvironmentState(info: StateModificationInfo) {
        rememberThisInstanceState(info, FieldState.FINAL)
        rememberArgumentsState(info, FieldState.FINAL)
    }

    private fun rememberArgumentsState(info: StateModificationInfo, state: PythonCgFieldStateManager.FieldState) {
        for ((i, argument) in context.methodArguments.withIndex()) {
            if (i > info.parameters.lastIndex) break

            val modifiedFields = info.parameters[i]
            saveFieldsState(argument, modifiedFields, statesCache.arguments[i], state)
        }
    }

    private fun rememberThisInstanceState(info: StateModificationInfo, state: PythonCgFieldStateManager.FieldState) {
        saveFieldsState(context.thisInstance!!, info.thisInstance, statesCache.thisInstance, state)
    }

    private fun saveFieldsState(
        owner: CgValue,
        modifiedFields: List<ModifiedField>,
        cache: FieldStateCache,
        state: FieldState
    ) {
        if (modifiedFields.isEmpty()) return
        emptyLineIfNeeded()
        val fields = when (state) {
            FieldState.INITIAL -> modifiedFields
                .filter { it.path.elements.isNotEmpty() }
            FieldState.FINAL -> modifiedFields
        }
        for ((path, before, after) in fields) {
            val customName = state.variablePrefix
            val variable = variableForFieldState(owner, path, customName)
            when (state) {
                FieldState.INITIAL -> cache.before[path] = CgFieldState(variable, before)
                FieldState.FINAL -> cache.after[path] = CgFieldState(variable, after)
            }
        }
    }

    private fun variableForFieldState(owner: CgValue, fieldPath: FieldPath, customName: String? = null): CgVariable {
        return owner.getFieldBy(fieldPath, customName)
    }

    private fun CgExpression.getFieldBy(fieldPath: FieldPath, customName: String? = null): CgVariable {
        val path = fieldPath.elements
        var lastAccessibleIndex = path.lastIndex

        // type of current accessed element, starting from current expression and followed by elements from path
        var curType = type
        for ((index, fieldPathElement) in path.withIndex()) {
            when (fieldPathElement) {
                is FieldAccess -> {
                    // if previous field has type that does not have current field, this field is inaccessible
                    if (index > 0 && !path[index - 1].type.hasField(fieldPathElement.field)) {
                        lastAccessibleIndex = index - 1
                        break
                    }
                }
                is ArrayElementAccess -> {
                    // cannot use direct array access from not array type
                    if (!curType.isArray) {
                        lastAccessibleIndex = index - 1
                        break
                    }
                }
            }

            curType = fieldPathElement.type
        }

        var index = 0
        var currentFieldType = this.type

        val lastPublicAccessor = generateSequence(this) { prev ->
            if (index > lastAccessibleIndex) return@generateSequence null
            val newElement = path[index++]
            currentFieldType = newElement.type
            when (newElement) {
                is FieldAccess -> prev[newElement.field]
                is ArrayElementAccess -> throw IllegalArgumentException()
            }
        }.last()

        if (index == path.size) {
            return newVar(currentFieldType, customName) { lastPublicAccessor }
        }

        val lastPublicFieldVariable = lastPublicAccessor as? CgVariable ?: newVar(currentFieldType) { lastPublicAccessor }
        return generateSequence(lastPublicFieldVariable) { prev ->
            if (index > path.lastIndex) return@generateSequence null
            val passedPath = FieldPath(path.subList(0, index + 1))
            val name = if (index == path.lastIndex) customName else ""

            val newElement = path[index++]
            val expression = when (newElement) {
                is FieldAccess -> {
                    val fieldId = newElement.field
                    utilsClassId[getFieldValue](prev, fieldId.declaringClass.name, fieldId.name)
                }
                is ArrayElementAccess -> { throw IllegalArgumentException() }
            }
            newVar(newElement.type, name) { expression }
        }.last()
    }

}