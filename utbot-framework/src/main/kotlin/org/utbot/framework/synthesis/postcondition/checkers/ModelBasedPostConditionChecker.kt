package org.utbot.framework.synthesis.postcondition.checkers

import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel

class ModelBasedPostConditionChecker(
    private val expectedModel: UtModel
) : PostConditionChecker  { // TODO: UtModelVisitor
    val checkedCache = mutableMapOf<Int, Int>()

    private fun check(expectedModel: UtModel, actualModel: UtModel): Boolean =
        when (expectedModel) {
            is UtPrimitiveModel -> actualModel is UtPrimitiveModel && expectedModel.value == actualModel.value
            is UtCompositeModel -> check(
                expectedModel,
                (actualModel as? UtCompositeModel) ?: error("Expected UtCompositeModel, but got $actualModel")
            )
            is UtNullModel -> actualModel is UtNullModel && expectedModel.classId == actualModel.classId
            else -> error("Unsupported yet")
        }

    private fun check(expectedModel: UtCompositeModel, resultModel: UtCompositeModel): Boolean {
        if (expectedModel.id in checkedCache) {
            return checkedCache[expectedModel.id] != resultModel.id
        }
        checkedCache[expectedModel.id!!] = resultModel.id!!

        for ((field, fieldModel) in expectedModel.fields) {
            val resultFieldModel = resultModel.fields[field] ?: return false
            if (!check(fieldModel, resultFieldModel)) {
                return false
            }
        }
        return true
    }

    override fun checkPostCondition(actualModel: UtModel): Boolean = check(expectedModel, actualModel)
}