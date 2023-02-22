package org.utbot.python.framework.fields

import org.utbot.framework.fields.FieldPath
import org.utbot.framework.fields.FieldStatesInfo
import org.utbot.framework.fields.ModifiedField
import org.utbot.framework.fields.ModifiedFields
import org.utbot.framework.fields.StateModificationInfo
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
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.util.UtModelVisitor
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.python.framework.api.python.PythonTreeModel

class PythonExecutionStateAnalyzer(val execution: UtExecution) {
    fun findModifiedFields(): StateModificationInfo {
        return StateModificationInfo()
            .analyzeThisInstance()
            .analyzeParameters()
    }

    private fun StateModificationInfo.analyzeThisInstance(): StateModificationInfo {
        return when (execution) {
            is UtFuzzedExecution -> {
                this
            }

            else -> {
                this
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

    private fun analyzeModelStates(before: UtModel, after: UtModel): FieldStatesInfo {
        if (before is PythonTreeModel && after is PythonTreeModel) {
            val fieldsBefore = mutableMapOf<FieldPath, UtModel>()
            val fieldsAfter = mutableMapOf<FieldPath, UtModel>()

            val dataBefore = FieldData(FieldsVisitorMode.BEFORE, fieldsBefore)
            before.accept(FieldStateVisitor(), dataBefore)

            val dataAfter = FieldData(FieldsVisitorMode.AFTER, fieldsAfter, previousFields = fieldsBefore)
            after.accept(FieldStateVisitor(), dataAfter)

            return FieldStatesInfo(fieldsBefore, fieldsAfter)
        } else {
            throw IllegalArgumentException("Support only PythonTreeModel")
        }
    }

    private fun getModifiedFields(info: FieldStatesInfo): ModifiedFields {
        val (fieldsBefore, fieldsAfter) = info
        val fields = fieldsBefore.keys intersect fieldsAfter.keys
        return fields.associateWith { fieldsBefore[it]!! to fieldsAfter[it]!! }
            .filterValues { (before, after) -> before != after }
            .map { (path, states) -> ModifiedField(path, states.first, states.second) }
    }
}

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
    private fun recordFieldState(data: FieldData, model: UtModel) {
        val fields = data.fields
        val path = data.path
        fields[path] = model
    }

    override fun visit(element: UtModel, data: FieldData) {
        if (element is PythonTreeModel) {
            recordFieldState(data, element)
        } else {
            throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
        }
    }

    override fun visit(element: UtNullModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }

    override fun visit(element: UtPrimitiveModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }

    override fun visit(element: UtVoidModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }

    override fun visit(element: UtClassRefModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }

    override fun visit(element: UtEnumConstantModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }

    override fun visit(element: UtArrayModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }

    override fun visit(element: UtAssembleModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }

    override fun visit(element: UtCompositeModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }

    override fun visit(element: UtLambdaModel, data: FieldData) {
        throw IllegalArgumentException("Invalid non-python UtModel: ${element.classId}")
    }
}

fun <D> UtModel.accept(visitor: UtModelVisitor<D>, data: D) = visitor.run {
    when (val element = this@accept) {
        is PythonTreeModel -> visit(element, data)
        else -> throw UnsupportedOperationException()
    }
}
