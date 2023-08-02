package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.builtin.spyClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.framework.SpyFrameworkManager
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtModelWithCompositeOrigin
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.canBeSpied
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.getBeanNameOrNull
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.isSubtypeOf

sealed interface CgClassFieldManager : CgContextOwner {

    val annotationType: ClassId

    fun constructVariableForField(model: UtModel, modelVariable: CgValue)

    fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean

    fun getBaseVarName(model: UtModel): String?
}

abstract class CgClassFieldManagerImpl(context: CgContext) :
    CgClassFieldManager,
    CgContextOwner by context {

    val variableConstructor: CgSpringVariableConstructor by lazy {
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor
    }

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)

    val mockFrameworkManager = CgComponents.getMockFrameworkManagerBy(context)
    val spyFrameworkManager = SpyFrameworkManager(context)

    fun findCgValueByModel(model: UtModel, setOfModels: Set<UtModelWrapper>?): CgValue? {
        val key = setOfModels?.find { it == model.wrap() } ?: return null
        return valueByUtModelWrapper[key]
    }

    override fun getBaseVarName(model: UtModel): String? {
        return nameGenerator.nameFrom(model.classId)
    }
}

abstract class CgUnitTestsClassFieldManager(context: CgContext) : CgClassFieldManagerImpl(context){

    override fun fieldWithAnnotationIsRequired(classId: ClassId) =
        classUnderTest.allDeclaredFieldIds.filter { classId.isSubtypeOf(it.type) }.toList().size == 1
}


class CgInjectingMocksFieldsManager(val context: CgContext) : CgClassFieldManagerImpl(context) {

    override val annotationType = injectMocksClassId

    override fun constructVariableForField(model: UtModel, modelVariable: CgValue) {
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

            // is variable spied by @Spy annotation
            val isSpied = findCgValueByModel(fieldModel, variableConstructor.annotatedModelGroups[spyClassId]) != null

            // If field model is a mock model and is mocked by @Mock annotation in classFields or is spied by @Spy annotation,
            // it is set in the connected with instance under test automatically via @InjectMocks.
            // Otherwise, we need to set this field manually.
            if ((!fieldModel.isMockModel() || !isMocked) && !isSpied) {
                variableConstructor.setFieldValue(modelVariable, fieldId, variableForField)
            }
        }
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}

class CgMockedFieldsManager(context: CgContext) : CgUnitTestsClassFieldManager(context) {

    override val annotationType = mockClassId

    override fun constructVariableForField(model: UtModel, modelVariable: CgValue) {
        if (!model.isMockModel()) {
            error("isMockModel was expected, but $model was found")
        }

        mockFrameworkManager.createMockForVariable(
            model as UtCompositeModel,
            modelVariable as CgVariable,
        )

        for ((fieldId, fieldModel) in model.fields) {
            val variableForField = variableConstructor.getOrCreateVariable(fieldModel)
            variableConstructor.setFieldValue(modelVariable, fieldId, variableForField)
        }
    }
}

class CgSpiedFieldsManager(context: CgContext) : CgUnitTestsClassFieldManager(context) {
    override val annotationType = spyClassId

    override fun constructVariableForField(model: UtModel, modelVariable: CgValue) {
        if (!model.canBeSpied()) {
            error("canBeSpied model was expected, but $model was found")
        }
        spyFrameworkManager.spyForVariable(
            model as UtAssembleModel,
        )
    }

    override fun getBaseVarName(model: UtModel): String {
        return super.getBaseVarName(model) + "Spy"
    }

}

class CgAutowiredFieldsManager(context: CgContext) : CgClassFieldManagerImpl(context) {

    override val annotationType = autowiredClassId

    override fun constructVariableForField(model: UtModel, modelVariable: CgValue) {
        when {
            model.isAutowiredFromContext() -> {
                variableConstructor.constructAssembleForVariable(model as UtAssembleModel)
            }

            else -> error("Trying to autowire model $model but it is not appropriate")
        }
    }

    override fun getBaseVarName(model: UtModel): String? {
        return model.getBeanNameOrNull()
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}

class ClassFieldManagerFacade(context: CgContext) : CgContextOwner by context {

    private val injectingMocksFieldsManager = CgInjectingMocksFieldsManager(context)
    private val mockedFieldsManager = CgMockedFieldsManager(context)
    private val spiedFieldsManager = CgSpiedFieldsManager(context)
    private val autowiredFieldsManager = CgAutowiredFieldsManager(context)

    fun constructVariableForField(
        model: UtModel,
    ): CgValue? {
        val annotationManagers =
            listOf(injectingMocksFieldsManager, mockedFieldsManager, spiedFieldsManager, autowiredFieldsManager)

        annotationManagers.forEach { manager ->
            val annotatedModelGroups = manager.variableConstructor.annotatedModelGroups

            val alreadyCreatedVariable = manager.findCgValueByModel(model, annotatedModelGroups[manager.annotationType])

            if (alreadyCreatedVariable != null) {
                manager.constructVariableForField(model, alreadyCreatedVariable)
                return alreadyCreatedVariable
            }
        }

        return null
    }
}