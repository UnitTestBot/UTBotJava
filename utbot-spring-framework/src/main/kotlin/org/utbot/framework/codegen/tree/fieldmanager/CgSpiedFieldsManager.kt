package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.builtin.spyClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.services.framework.SpyFrameworkManager
import org.utbot.framework.codegen.tree.fieldmanager.MockitoInjectionUtils.canBeInjectedByTypeInto
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.canBeSpied
import org.utbot.framework.plugin.api.isMockModel

class CgSpiedFieldsManager(context: CgContext) : CgAbstractClassFieldManager(context) {

    init {
        relevantFieldManagers += this
    }

    override val annotationType = spyClassId

    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = modelGroupsProvider.collectModelsByOrigin(testClassModel)

        val dependentMockModels =
            modelsByOrigin.thisInstanceDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.isMockModel() && cgModel !in modelsByOrigin.thisInstanceModels
                }

        val dependentSpyModels =
            modelsByOrigin.thisInstanceDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.canBeSpied() &&
                            cgModel !in modelsByOrigin.thisInstanceModels &&
                            cgModel !in dependentMockModels
                }

        return constructFieldsWithAnnotation(dependentSpyModels)
    }


    private val spyFrameworkManager = SpyFrameworkManager(context)

    override fun useVariableForModel(model: UtModel, variable: CgValue) {
        if (!model.canBeSpied()) {
            error("$model does not represent a spy")
        }
        spyFrameworkManager.spyForVariable(
            model as UtAssembleModel,
        )
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean =
        classId.canBeInjectedByTypeInto(classUnderTest)

    override fun constructBaseVarName(model: UtModel): String = super.constructBaseVarName(model) + "Spy"
}