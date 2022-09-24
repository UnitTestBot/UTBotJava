package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.constructor.builtin.forName
import org.utbot.framework.codegen.model.constructor.builtin.getArrayElement
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.CgFieldState
import org.utbot.framework.codegen.model.constructor.util.CgStatementConstructor
import org.utbot.framework.codegen.model.constructor.util.FieldStateCache
import org.utbot.framework.codegen.model.constructor.util.classCgClassId
import org.utbot.framework.codegen.model.constructor.util.getFieldVariableName
import org.utbot.framework.codegen.model.constructor.util.getStaticFieldVariableName
import org.utbot.framework.codegen.model.constructor.util.needExpectedDeclaration
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.at
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.codegen.model.util.canBeReadFrom
import org.utbot.framework.codegen.model.util.stringLiteral
import org.utbot.framework.fields.ArrayElementAccess
import org.utbot.framework.fields.FieldAccess
import org.utbot.framework.fields.FieldPath
import org.utbot.framework.fields.ModifiedFields
import org.utbot.framework.fields.StateModificationInfo
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.util.hasField
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.util.hasThisInstance
import org.utbot.fuzzer.UtFuzzedExecution
import java.lang.reflect.Array

internal interface CgFieldStateManager {
    fun rememberInitialEnvironmentState(info: StateModificationInfo)
    fun rememberFinalEnvironmentState(info: StateModificationInfo)
}

internal class CgFieldStateManagerImpl(val context: CgContext)
    : CgContextOwner by context,
        CgFieldStateManager,
        CgCallableAccessManager by CgComponents.getCallableAccessManagerBy(context),
        CgStatementConstructor by CgComponents.getStatementConstructorBy(context) {

    override fun rememberInitialEnvironmentState(info: StateModificationInfo) {
        rememberThisInstanceState(info, FieldState.INITIAL)
        rememberArgumentsState(info, FieldState.INITIAL)
        rememberStaticFieldsState(info, FieldState.INITIAL)
    }

    override fun rememberFinalEnvironmentState(info: StateModificationInfo) {
        rememberThisInstanceState(info, FieldState.FINAL)
        rememberArgumentsState(info, FieldState.FINAL)
        rememberStaticFieldsState(info, FieldState.FINAL)
    }

    /**
     * [variablePrefix] is used as a name prefix for variables
     * that contain the initial or final states of some fields.
     */
    private enum class FieldState(val variablePrefix: String) {
        INITIAL("initial"),
        FINAL("final")
    }

    private fun rememberThisInstanceState(info: StateModificationInfo, state: FieldState) {
        when (currentExecution) {
            is UtSymbolicExecution -> {
                if (!(currentExecution!! as UtSymbolicExecution).hasThisInstance()) {
                    return
                }
                val thisInstance = context.thisInstance!!
                val modifiedFields = info.thisInstance
                // by now this instance variable must have already been created
                saveFieldsState(thisInstance, modifiedFields, statesCache.thisInstance, state)
            }
            is UtFuzzedExecution -> {
                return
            }
            else -> {
                return
            }
        }
    }

    private fun rememberArgumentsState(info: StateModificationInfo, state: FieldState) {
        // by now variables of all arguments must have already been created
        for ((i, argument) in context.methodArguments.withIndex()) {
            if (i > info.parameters.lastIndex) break

            // TODO no one understands what is going on here; need to rewrite it one day or add docs
            val modifiedFields = info.parameters[i]
            saveFieldsState(argument, modifiedFields, statesCache.arguments[i], state)
        }
    }

    private fun rememberStaticFieldsState(info: StateModificationInfo, state: FieldState) {
        for ((classId, modifiedStaticFields) in info.staticFields) {
            saveStaticFieldsState(classId, modifiedStaticFields, state)
            statesCache.classesWithStaticFields[classId]!!.before
        }
    }

    private fun getStaticFieldStateVariableName(owner: ClassId, path: FieldPath, state: FieldState): String =
            state.variablePrefix + getStaticFieldVariableName(owner, path).capitalize()

    private fun getFieldStateVariableName(owner: CgValue, path: FieldPath, state: FieldState): String =
            state.variablePrefix + getFieldVariableName(owner, path).capitalize()

    /**
     * For initial state we only need to create variables for ref type field values,
     * because in order to assert inequality of ref type fields we need both initial
     * and final state variables.
     *
     * On the contrary, when the field is not of ref type,
     * then we will only need its final state and the assertion will make sure
     * that this final state is equal to its expected value.
     *
     * Assertion examples:
     * - ref type fields:
     *
     *    `assertFalse(initialState == finalState);`
     *
     * - non-ref type fields:
     *
     *    `assertEquals(5, finalState);`
     */
    private fun saveFieldsState(
        owner: CgValue,
        modifiedFields: ModifiedFields,
        cache: FieldStateCache,
        state: FieldState
    ) {
        if (modifiedFields.isEmpty()) return
        emptyLineIfNeeded()
        val fields = when (state) {
            FieldState.INITIAL -> modifiedFields
                    .filter { it.path.elements.isNotEmpty() && it.path.fieldType.isRefType }
                    .filter { needExpectedDeclaration(it.after) }
            FieldState.FINAL -> modifiedFields
        }
        for ((path, before, after) in fields) {
            val customName = getFieldStateVariableName(owner, path, state)
            val variable = variableForFieldState(owner, path, customName)
            when (state) {
                FieldState.INITIAL -> cache.before[path] = CgFieldState(variable, before)
                FieldState.FINAL -> cache.after[path] = CgFieldState(variable, after)
            }
        }
    }

    private fun saveStaticFieldsState(
            owner: ClassId,
            modifiedFields: ModifiedFields,
            state: FieldState
    ) {
        if (modifiedFields.isNotEmpty()) {
            emptyLineIfNeeded()
        }
        for (modifiedField in modifiedFields) {
            val (path, before, after) = modifiedField
            val customName = getStaticFieldStateVariableName(owner, path, state)
            val variable = variableForStaticFieldState(owner, path, customName)
            val cache = statesCache.classesWithStaticFields[owner]!!
            when (state) {
                FieldState.INITIAL -> cache.before[path] = CgFieldState(variable, before)
                FieldState.FINAL -> cache.after[path] = CgFieldState(variable, after)
            }
        }
    }

    private fun CgExpression.getFieldBy(fieldPath: FieldPath, customName: String? = null): CgVariable {
        val path = fieldPath.elements
        var lastAccessibleIndex = path.lastIndex

        // type of current accessed element, starting from current expression and followed by elements from path
        var curType = type
        for ((index, fieldPathElement) in path.withIndex()) {
            when (fieldPathElement) {
                is FieldAccess -> {
                    if (!fieldPathElement.field.canBeReadFrom(context)) {
                        lastAccessibleIndex = index - 1
                        break
                    }

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
                is ArrayElementAccess -> prev.at(newElement.index)
            }
        }.last()

        if (index == path.size) {
            return newVar(currentFieldType, customName) { lastPublicAccessor }
        }

        val lastPublicFieldVariable = lastPublicAccessor as? CgVariable ?: newVar(currentFieldType) { lastPublicAccessor }
        return generateSequence(lastPublicFieldVariable) { prev ->
            if (index > path.lastIndex) return@generateSequence null
            val passedPath = FieldPath(path.subList(0, index + 1))
            val name = if (index == path.lastIndex) customName else getFieldVariableName(prev, passedPath)
            val expression = when (val newElement = path[index++]) {
                is FieldAccess -> {
                    val fieldId = newElement.field
                    utilsClassId[getFieldValue](prev, fieldId.declaringClass.name, fieldId.name)
                }
                is ArrayElementAccess -> {
                    Array::class.id[getArrayElement](prev, newElement.index)
                }
            }
            newVar(objectClassId, name) { expression }
        }.last()
    }

    private fun variableForFieldState(owner: CgValue, fieldPath: FieldPath, customName: String? = null): CgVariable {
        return owner.getFieldBy(fieldPath, customName)
    }

    private fun variableForStaticFieldState(owner: ClassId, fieldPath: FieldPath, customName: String?): CgVariable {
        val firstField = (fieldPath.elements.first() as FieldAccess).field
        val firstAccessor = if (owner.isAccessibleFrom(testClassPackageName) && firstField.canBeReadFrom(context)) {
            owner[firstField]
        } else {
            // TODO: there is a function getClassOf() for these purposes, but it is not accessible from here for now
            val ownerClass = if (owner isAccessibleFrom testClassPackageName) {
                CgGetJavaClass(owner)
            } else {
                newVar(classCgClassId) { Class::class.id[forName](owner.name) }
            }
            newVar(objectClassId) { utilsClassId[getStaticFieldValue](ownerClass, stringLiteral(firstField.name)) }
        }
        val path = fieldPath.elements
        val remainingPath = fieldPath.copy(elements = path.drop(1))
        return firstAccessor.getFieldBy(remainingPath, customName)
    }
}