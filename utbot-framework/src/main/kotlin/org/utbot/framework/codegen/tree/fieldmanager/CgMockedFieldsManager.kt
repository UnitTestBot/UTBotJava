package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.tree.CgComponents
import org.utbot.framework.codegen.tree.fieldmanager.MockitoInjectionUtils.canBeInjectedByTypeInto
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.isMockModel

class CgMockedFieldsManager(context: CgContext) : CgAbstractClassFieldManager(context) {
    init {
        relevantFieldManagers += this
    }

    override val annotationType = mockClassId
    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = modelGroupsProvider.collectModelsByOrigin(testClassModel)

        val dependentMockModels =
            modelsByOrigin.thisInstanceDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.isMockModel() && cgModel !in modelsByOrigin.thisInstanceModels
                }

        return constructFieldsWithAnnotation(dependentMockModels)
    }

    private val mockFrameworkManager = CgComponents.getMockFrameworkManagerBy(context)
    override fun useVariableForModel(model: UtModel, variable: CgValue) {
        if (!model.isMockModel()) {
            error("$model does not represent a mock")
        }

        mockFrameworkManager.createMockForVariable(
            model as UtCompositeModel,
            variable as CgVariable,
        )

        for ((fieldId, fieldModel) in model.fields) {
            val variableForField = variableConstructor.getOrCreateVariable(fieldModel)
            variableConstructor.setFieldValue(variable, fieldId, variableForField)
        }
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean =
        classId.canBeInjectedByTypeInto(classUnderTest)
}