package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext

sealed interface CgClassFieldManager : CgContextOwner {

    val annotationType: ClassId

    fun constructVariableForField(model: UtModel, modelVariable: CgValue): CgValue
}

abstract class CgClassFieldManagerImpl(context: CgContext) :
    CgClassFieldManager,
    CgContextOwner by context {

    val variableConstructor: CgSpringVariableConstructor by lazy {
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor
    }

    fun findCgValueByModel(model: UtModel, setOfModels: Set<UtModelWrapper>?): CgValue? {
        val key = setOfModels?.find { it == model.wrap() } ?: return null
        return valueByUtModelWrapper[key]
    }
}

class CgInjectingMocksFieldsManager(val context: CgContext) : CgClassFieldManagerImpl(context) {

    override val annotationType = injectMocksClassId

    override fun constructVariableForField(model: UtModel, modelVariable: CgValue): CgValue {
        val modelFields = when (model) {
            is UtCompositeModel -> model.fields
            is UtAssembleModel -> model.origin?.fields
            else -> null
        }

        modelFields?.forEach { (fieldId, fieldModel) ->
            // creating variables for modelVariable fields
            val variableForField = variableConstructor.getOrCreateVariable(fieldModel)

            // is variable mocked by @Mock annotation
            val isMocked = findCgValueByModel(fieldModel, variableConstructor.annotatedModelGroups[mockClassId]) != null

            // If field model is a mock model and is mocked by @Mock annotation in classFields, it is set in the connected with instance under test automatically via @InjectMocks;
            // Otherwise we need to set this field manually.
            if (!fieldModel.isMockModel() || !isMocked) {
                variableConstructor.setFieldValue(modelVariable, fieldId, variableForField)
            }
        }

        return modelVariable
    }

}

class CgMockedFieldsManager(context: CgContext) : CgClassFieldManagerImpl(context) {

    override val annotationType = mockClassId

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

class CgAutowiredFieldsManager(context: CgContext) : CgClassFieldManagerImpl(context) {

    override val annotationType = autowiredClassId

    override fun constructVariableForField(model: UtModel, modelVariable: CgValue): CgValue {
        return when {
            model.isAutowiredFromContext() -> {
                variableConstructor.constructAssembleForVariable(model as UtAssembleModel)
            }

            else -> error("Trying to autowire model $model but it is not appropriate")
        }
    }
}

class ClassFieldManagerFacade(context: CgContext) : CgContextOwner by context {

    private val injectingMocksFieldsManager = CgInjectingMocksFieldsManager(context)
    private val mockedFieldsManager = CgMockedFieldsManager(context)
    private val autowiredFieldsManager = CgAutowiredFieldsManager(context)

    fun constructVariableForField(
        model: UtModel,
    ): CgValue? {
        val annotationManagers = listOf(injectingMocksFieldsManager, mockedFieldsManager, autowiredFieldsManager)

        annotationManagers.forEach { manager ->
            val annotatedModelGroups = manager.variableConstructor.annotatedModelGroups

            val alreadyCreatedVariable = manager.findCgValueByModel(model, annotatedModelGroups[manager.annotationType])

            if (alreadyCreatedVariable != null) {
                return manager.constructVariableForField(model, alreadyCreatedVariable)
            }
        }

        return null
    }
}