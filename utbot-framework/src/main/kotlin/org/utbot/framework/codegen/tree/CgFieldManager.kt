package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext

interface CgFieldManager : CgContextOwner {
    val annotation: ClassId

    val variableConstructor: CgSpringVariableConstructor

    fun constructVariableForField(model: UtModel, modelVariable: CgValue): CgValue

    fun findCgValueByModel(model: UtModel, setOfModels: Set<UtModelWrapper>?): CgValue? {
        val key = setOfModels?.find { it == model.wrap() } ?: return null
        return valueByUtModelWrapper[key]
    }
}

class CgInjectingMocksFieldsManager(context: CgContext) :
    CgFieldManager,
    CgContextOwner by context
{
    override val annotation = injectMocksClassId

    override val variableConstructor: CgSpringVariableConstructor =
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor


    override fun constructVariableForField(model: UtModel, modelVariable: CgValue): CgValue {
        val modelFields = when (model) {
            is UtCompositeModel -> model.fields
            is UtAssembleModel -> model.origin?.fields
            else -> null
        }

        modelFields?.forEach { (fieldId, fieldModel) ->
            //creating variables for modelVariable fields
            val variableForField = variableConstructor.getOrCreateVariable(fieldModel)

            // If field model is a mock, it is set in the connected with instance under test automatically via @InjectMocks;
            // Otherwise we need to set this field manually.
            if (!fieldModel.isMockModel()) {
                variableConstructor.setFieldValue(modelVariable, fieldId, variableForField)
            }
        }

        return modelVariable
    }

}

class CgMockedFieldsManager(context: CgContext) :
    CgFieldManager,
    CgContextOwner by context
{
    override val annotation = mockClassId

    override val variableConstructor: CgSpringVariableConstructor =
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor

    override fun constructVariableForField(model: UtModel, modelVariable: CgValue): CgValue {
        if (model.isMockModel()) {
            variableConstructor.mockFrameworkManager.createMockForVariable(
                model as UtCompositeModel,
                modelVariable as CgVariable,
            )
        }
        return modelVariable
    }

}

class CgAutowiredFieldsManager(context: CgContext) :
    CgFieldManager,
    CgContextOwner by context
{
    override val annotation = autowiredClassId

    override val variableConstructor: CgSpringVariableConstructor =
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor


    override fun constructVariableForField(model: UtModel, modelVariable: CgValue): CgValue {
        return when {
            model.isAutowiredFromContext() -> {
                variableConstructor.constructAssembleForVariable(model as UtAssembleModel)
            }
            else -> error("Trying to autowire model $model but it is not appropriate")
        }
    }

}

class FieldManagerFacade(
    context: CgContext,
    private val annotatedModelVariables: MutableMap<ClassId, MutableSet<UtModelWrapper>>
) :
    CgContextOwner by context
{
    private val injectingMocksFieldsManager = CgInjectingMocksFieldsManager(context)
    private val mockedFieldsManager = CgMockedFieldsManager(context)
    private val autowiredFieldsManager = CgAutowiredFieldsManager(context)

    fun constructVariableForField(model: UtModel): CgValue? {
        listOf(injectingMocksFieldsManager, mockedFieldsManager, autowiredFieldsManager).forEach { manager ->
            val alreadyCreatedVariable = manager.findCgValueByModel(model, annotatedModelVariables[manager.annotation])

            if (alreadyCreatedVariable != null) {
                return manager.constructVariableForField(model, alreadyCreatedVariable)
            }
        }
        return null
    }
}