package org.utbot.framework.codegen.tree

import com.jetbrains.rd.util.firstOrNull
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.isMockModel

class CgSpringVariableConstructor(context: CgContext) : CgVariableConstructor(context) {
    val injectedMocksModelsVariables: MutableMap<Set<UtModel>, CgValue> = mutableMapOf()
    val mockedModelsVariables: MutableMap<Set<UtModel>, CgValue> = mutableMapOf()

    private val mockFrameworkManager = CgComponents.getMockFrameworkManagerBy(context)

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

    private fun findCgValueByModel(model: UtModel, modelToValueMap: Map<Set<UtModel>, CgValue>): CgValue? =
        modelToValueMap
            .filter { it.key.contains(model) }
            .asSequence()
            .singleOrNull()
            ?.value
}