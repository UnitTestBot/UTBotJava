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
import org.utbot.framework.plugin.api.UtModelWithCompositeOrigin
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext
import java.util.Collections.max
import kotlin.collections.HashMap

sealed interface CgClassFieldManager : CgContextOwner {

    val annotationType: ClassId

    fun constructVariableForField(model: UtModel, modelVariable: CgValue): CgValue

    fun fieldWithAnnotationIsRequired(modelWrappers: Set<UtModelWrapper>): Boolean
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
            is UtModelWithCompositeOrigin -> model.origin?.fields
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

    override fun fieldWithAnnotationIsRequired(modelWrappers: Set<UtModelWrapper>): Boolean = true
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

    override fun fieldWithAnnotationIsRequired(modelWrappers: Set<UtModelWrapper>): Boolean {
        // group [listOfUtModels] by `testSetId` and `executionId`
        // to check how many instances of one type are used in each execution
        val modelWrappersByExecutions = modelWrappers
            .groupByTo(HashMap()) { Pair(it.testSetId, it.executionId) }

        // maximal count of instances of the same type amount in one execution
        // we use `modelTagName` in order to distinguish mock models by their name
        val maxCountOfInstancesOfTheSameTypeByExecution = max(modelWrappersByExecutions.map { (_, modelsList) -> modelsList.size })

        // if [maxCountOfInstancesOfTheSameTypeByExecution] is 1, then we mock variable by @Mock annotation
        // Otherwise we will mock variable by simple mock later
        return maxCountOfInstancesOfTheSameTypeByExecution == 1
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

    override fun fieldWithAnnotationIsRequired(modelWrappers: Set<UtModelWrapper>): Boolean = true
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