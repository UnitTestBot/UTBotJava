package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtAutowiredModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.jClass

class CgSpringVariableConstructor(context: CgContext) : CgVariableConstructor(context) {
    val injectedMocksModelsVariables: MutableMap<Set<UtModel>, CgValue> = mutableMapOf()
    val mockedModelsVariables: MutableMap<Set<UtModel>, CgValue> = mutableMapOf()

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        if (model is UtAutowiredModel)
            // TODO properly render
            CgVariable(name ?: model.classId.jClass.simpleName.let { it[0].lowercase() + it.drop(1) }, model.classId)

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

    private fun findCgValueByModel(modelToFind: UtModel, modelToValueMap: Map<Set<UtModel>, CgValue>): CgValue? =
        // Here we really need to compare models by reference.
        // Standard equals is not appropriate because two models from different execution may have same `id`.
        // Equals on `ModelId` is not appropriate because injected items from different execution have same `executionId`.
        modelToValueMap
            .filter { models -> models.key.any { it === modelToFind } }
            .entries
            .singleOrNull()
            ?.value
}