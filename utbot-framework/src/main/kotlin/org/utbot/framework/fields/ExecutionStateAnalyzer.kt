package org.utbot.framework.fields

import org.utbot.common.WorkaroundReason
import org.utbot.common.doNotRun
import org.utbot.common.unreachableBranch
import org.utbot.common.workaround
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.util.UtModelVisitor
import org.utbot.framework.util.hasThisInstance
import org.utbot.fuzzer.UtFuzzedExecution

class ExecutionStateAnalyzer(val execution: UtExecution) {
    fun findModifiedFields(): StateModificationInfo {
        return StateModificationInfo()
            .analyzeThisInstance()
            .analyzeParameters()
            .analyzeStatics()
    }

    private fun StateModificationInfo.analyzeThisInstance(): StateModificationInfo {
        when (execution) {
            is UtSymbolicExecution -> {
                if (!execution.hasThisInstance()) {
                    return this
                }
                val thisInstanceBefore = execution.stateBefore.thisInstance!!
                val thisInstanceAfter = execution.stateAfter.thisInstance!!
                val info = analyzeModelStates(thisInstanceBefore, thisInstanceAfter)
                val modifiedFields = getModifiedFields(info)
                return this.copy(thisInstance = modifiedFields)
            }
            is UtFuzzedExecution -> {
                return this
            }
            else -> {
                return this
            }
        }
    }

    private fun StateModificationInfo.analyzeParameters(): StateModificationInfo {
        val parametersBefore = execution.stateBefore.parameters
        val parametersAfter = execution.stateAfter.parameters
        val parameterStates = parametersBefore zip parametersAfter
        val parameterModifications = mutableListOf<ModifiedFields>()
        for ((before, after) in parameterStates) {
            val info = analyzeModelStates(before, after)
            parameterModifications += getModifiedFields(info)
        }
        return this.copy(parameters = parameterModifications)
    }

    private fun StateModificationInfo.analyzeStatics(): StateModificationInfo {
        when (execution) {
            is UtSymbolicExecution -> {
                if (execution.stateAfter == MissingState) return this

                val staticsBefore = execution.stateBefore.statics
                val staticsAfter = execution.stateAfter.statics

                val staticFieldsByClass = execution.staticFields.groupBy { it.declaringClass }
                val modificationsByClass = mutableMapOf<ClassId, ModifiedFields>()
                for ((classId, fields) in staticFieldsByClass) {
                    val staticFieldModifications = mutableListOf<ModifiedField>()
                    for (field in fields) {
                        val before = staticsBefore[field]!!
                        val after = staticsAfter[field]!!
                        val path = FieldPath() + FieldAccess(field)
                        val info = analyzeModelStates(before, after, path)
                        staticFieldModifications += getModifiedFields(info)
                    }
                    modificationsByClass[classId] = staticFieldModifications
                }
                return this.copy(staticFields = modificationsByClass)
            }
            is UtFuzzedExecution -> {
                return this
            }
            else -> {
                return this
            }
        }
    }

    private fun analyzeModelStates(
        before: UtModel,
        after: UtModel,
        initialPath: FieldPath = FieldPath()
    ): FieldStatesInfo {
        var modelBefore = before

        if (before::class != after::class) {
            if (before is UtAssembleModel && after is UtCompositeModel && before.origin != null) {
                modelBefore = before.origin ?: unreachableBranch("We have already checked the origin for a null value")
            } else {
                doNotRun {
                    // it is ok because we might have modelBefore with some absent fields (i.e. statics), but
                    // modelAfter (constructed by concrete executor) will consist all these fields,
                    // therefore, AssembleModelGenerator won't be able to transform the given composite model

                    val reason = if (before is UtAssembleModel && after is UtCompositeModel) {
                        "ModelBefore is an AssembleModel and ModelAfter " +
                                "is a CompositeModel, but modelBefore doesn't have an origin model."
                    } else {
                        "The model before and the model after have different types: " +
                                "model before is ${before::class}, but model after is ${after::class}."
                    }

                    error("Cannot analyze fields modification. $reason")
                }

                // remove it when we will fix assemble models in the resolver JIRA:1464
                workaround(WorkaroundReason.IGNORE_MODEL_TYPES_INEQUALITY) {
                    return FieldStatesInfo(fieldsBefore = emptyMap(), fieldsAfter = emptyMap())
                }
            }
        }

        val fieldsBefore = mutableMapOf<FieldPath, UtModel>()
        val fieldsAfter = mutableMapOf<FieldPath, UtModel>()

        val dataBefore = FieldData(FieldsVisitorMode.BEFORE, fieldsBefore, initialPath)
        modelBefore.accept(FieldStateVisitor(), dataBefore)

        val dataAfter = FieldData(FieldsVisitorMode.AFTER, fieldsAfter, initialPath, previousFields = fieldsBefore)
        after.accept(FieldStateVisitor(), dataAfter)

        return FieldStatesInfo(fieldsBefore, fieldsAfter)
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    private fun getModifiedFields(info: FieldStatesInfo): ModifiedFields {
        val (fieldsBefore, fieldsAfter) = info
        val fields = fieldsBefore.keys intersect fieldsAfter.keys
        return fields.associateWith { fieldsBefore[it]!! to fieldsAfter[it]!! }
            .filterValues { (before, after) -> before != after }
            .map { (path, states) -> ModifiedField(path, states.first, states.second) }
    }
}

/**
 * @param mode can be either [FieldsVisitorMode.BEFORE] or [FieldsVisitorMode.AFTER]
 * meaning initial and final field states correspondingly.
 *
 * @param fields collected models of the fields visited during traversal
 *
 * @param path the sequence of field accesses required to reach the current field. For example `".a.b[0].c"`.
 *
 * @param previousFields field models collected during the previous traversal.
 * It is null when `mode == FieldsVisitorMode.BEFORE`.
 * Otherwise, if `mode == FieldsVisitorMode.AFTER`, then it contains the collected models.
 */
private data class FieldData(
    val mode: FieldsVisitorMode,
    val fields: MutableMap<FieldPath, UtModel>,
    val path: FieldPath = FieldPath(),
    val previousFields: Map<FieldPath, UtModel>? = null
)

private enum class FieldsVisitorMode {
    BEFORE,
    AFTER
}

private class FieldStateVisitor : UtModelVisitor<FieldData>() {
    override fun visit(element: UtModel, data: FieldData) {
        throw IllegalArgumentException(
            "FieldVisitor has reached the top of Cg elements hierarchy and did not find a method for ${element::class}"
        )
    }

    override fun visit(element: UtNullModel, data: FieldData) {
        recordFieldState(data, element)
    }

    override fun visit(element: UtPrimitiveModel, data: FieldData) {
        recordFieldState(data, element)
    }

    override fun visit(element: UtVoidModel, data: FieldData) {
        recordFieldState(data, element)
    }

    override fun visit(element: UtClassRefModel, data: FieldData) {
        recordFieldState(data, element)
    }

    override fun visit(element: UtEnumConstantModel, data: FieldData) {
        recordFieldState(data, element)
    }

    override fun visit(element: UtArrayModel, data: FieldData) {
        recordFieldState(data, element)
        if (refFieldChanged(data, element)) {
            return
        }
        val items = List(element.length) { element.stores[it] ?: element.constModel }
        val itemType = element.classId.elementClassId!!
        for ((i, item) in items.withIndex()) {
            val path = data.path + ArrayElementAccess(itemType, i)
            val newData = data.copy(path = path)
            item.accept(this, newData)
        }
    }

    override fun visit(element: UtAssembleModel, data: FieldData) {
        element.origin?.accept(this, data)
    }

    override fun visit(element: UtCompositeModel, data: FieldData) {
        recordFieldState(data, element)
        if (refFieldChanged(data, element)) {
            return
        }
        for ((id, field) in element.fields) {
            val path = data.path + FieldAccess(id)
            val newData = data.copy(path = path)
            field.accept(this, newData)
        }
    }

    override fun visit(element: UtLambdaModel, data: FieldData) {
        recordFieldState(data, element)
    }

    private fun recordFieldState(data: FieldData, model: UtModel) {
        val fields = data.fields
        val path = data.path
        fields[path] = model
    }

    private fun refFieldChanged(data: FieldData, model: UtReferenceModel): Boolean {
        if (data.mode == FieldsVisitorMode.BEFORE) {
            return false
        }
        // previous fields property must not be null when the mode is FieldsVisitorMode.AFTER
        val previousFields = data.previousFields!!
        val path = data.path

        // sometimes we don't have initial state of the field, e.g. if it is static and we didn't `touch` it
        // during the analysis, but the concrete executor included it in the modelAfter
        val initial = previousFields[path] ?: return false
        return initial != model
    }
}

fun <D> UtModel.accept(visitor: UtModelVisitor<D>, data: D) = visitor.run {
    when (val element = this@accept) {
        is UtClassRefModel -> visit(element, data)
        is UtEnumConstantModel -> visit(element, data)
        is UtNullModel -> visit(element, data)
        is UtPrimitiveModel -> visit(element, data)
        is UtReferenceModel -> visit(element, data)
        is UtVoidModel -> visit(element, data)
    }
}
