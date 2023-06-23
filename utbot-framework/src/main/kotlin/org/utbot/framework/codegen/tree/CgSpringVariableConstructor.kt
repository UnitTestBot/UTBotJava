package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.builtin.autowiredClassId
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.applicationContextClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext
import org.utbot.framework.plugin.api.util.stringClassId

class CgSpringVariableConstructor(context: CgContext) : CgVariableConstructor(context) {
    val annotatedModelVariables: MutableMap<ClassId, MutableSet<UtModelWrapper>> = mutableMapOf()

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val alreadyCreatedInjectMocks = findCgValueByModel(model, annotatedModelVariables[injectMocksClassId])
        if (alreadyCreatedInjectMocks != null) {
            val fields: Collection<UtModel> = when (model) {
                is UtCompositeModel -> model.fields.values
                is UtAssembleModel -> model.origin?.fields?.values ?: emptyList()
                else -> emptyList()
            }

            fields.forEach { getOrCreateVariable(it) }

            return alreadyCreatedInjectMocks
        }

        val alreadyCreatedMock = findCgValueByModel(model, annotatedModelVariables[mockClassId])
        if (alreadyCreatedMock != null) {
            if (model.isMockModel()) {
                mockFrameworkManager.createMockForVariable(
                    model as UtCompositeModel,
                    alreadyCreatedMock as CgVariable,
                )
            }

            return alreadyCreatedMock
        }

        val alreadyCreatedAutowired = findCgValueByModel(model, annotatedModelVariables[autowiredClassId])
        if (alreadyCreatedAutowired != null) {
            return when  {
                model.isAutowiredFromContext() -> {
                    super.constructAssembleForVariable(model as UtAssembleModel)
                }
                else -> error("Trying to autowire model $model but it is not appropriate")
            }
        }

        return when (model) {
            is UtSpringContextModel -> createApplicationContextVariable()
            else -> super.getOrCreateVariable(model, name)
        }
    }

    private fun createApplicationContextVariable(): CgValue {
        // This is a kind of HACK
        // Actually, applicationContext variable is useless as it is not used in the generated code.
        // However, this variable existence is required for autowired fields creation process.
        val applicationContextVariable = CgLiteral(stringClassId, "applicationContext")

        return applicationContextVariable.also {
            valueByUtModelWrapper[UtSpringContextModel.wrap()] = it
        }
    }

    private fun findCgValueByModel(model: UtModel, setOfModels: Set<UtModelWrapper>?): CgValue? {
        val key = setOfModels?.find { it == model.wrap() } ?: return null
        return valueByUtModelWrapper[key]
    }
}