package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.builtin.spyClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtModelWithCompositeOrigin
import org.utbot.framework.plugin.api.isMockModel

class CgInjectingMocksFieldsManager(
    val context: CgContext,
    private val mocksFieldsManager: CgMockedFieldsManager,
    private val spiesFieldsManager: CgSpiedFieldsManager,
    ) : CgAbstractClassFieldManager(context) {
    init {
        relevantFieldManagers += this
    }

    override val annotationType = injectMocksClassId

    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = modelGroupsProvider.collectModelsByOrigin(testClassModel)
        return constructFieldsWithAnnotation(modelsByOrigin.thisInstanceModels)
    }

    override fun useVariableForModel(model: UtModel, variable: CgValue) {
        val modelFields = when (model) {
            is UtCompositeModel -> model.fields
            is UtModelWithCompositeOrigin -> model.origin?.fields
            else -> null
        }

        modelFields?.forEach { (fieldId, fieldModel) ->
            // creating variables for modelVariable fields
            val variableForField = variableConstructor.getOrCreateVariable(fieldModel)

            // is variable mocked by @Mock annotation
            val isMocked = findCgValueByModel(fieldModel, mocksFieldsManager.annotatedModels) != null

            // is variable spied by @Spy annotation
            val isSpied = findCgValueByModel(fieldModel, spiesFieldsManager.annotatedModels) != null

            // If field model is a mock model and is mocked by @Mock annotation in classFields or is spied by @Spy annotation,
            // it is set in the connected with instance under test automatically via @InjectMocks.
            // Otherwise, we need to set this field manually.
            if ((!fieldModel.isMockModel() || !isMocked) && !isSpied) {
                variableConstructor.setFieldValue(variable, fieldId, variableForField)
            }
        }
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}