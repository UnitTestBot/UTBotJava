package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.isMockModel

class CgSpringVariableConstructor(context: CgContext) : CgVariableConstructor(context) {
    val injectedMocksModelsVariables: MutableSet<UtModelWrapper> = mutableSetOf()
    val mockedModelsVariables: MutableSet<UtModelWrapper> = mutableSetOf()

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val alreadyCreatedInjectMocks = findCgValueByModel(model, injectedMocksModelsVariables)
        if (alreadyCreatedInjectMocks != null) {
            val fields: Collection<UtModel> = when (model) {
                is UtCompositeModel -> model.fields.values
                is UtAssembleModel -> model.origin?.fields?.values ?: emptyList()
                else -> emptyList()
            }

            fields.forEach { getOrCreateVariable(it) }

            return alreadyCreatedInjectMocks
        }

        val alreadyCreatedMock = findCgValueByModel(model, mockedModelsVariables)
        if (alreadyCreatedMock != null) {
            if (model.isMockModel()) {
                mockFrameworkManager.createMockForVariable(
                    model as UtCompositeModel,
                    alreadyCreatedMock as CgVariable,
                )
            }

            return alreadyCreatedMock
        }

        return super.getOrCreateVariable(model, name)
    }

    private fun findCgValueByModel(model: UtModel, setOfModels: Set<UtModelWrapper>): CgValue? {
        val key = setOfModels.find { it == model.wrap() }
        return valueByUtModelWrapper[key]
    }
}