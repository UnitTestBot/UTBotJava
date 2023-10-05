package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.UtModelWrapper
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
import org.utbot.framework.plugin.api.spiedTypes
import org.utbot.framework.plugin.api.util.jClass

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

        val suitableSpyModels = getSuitableSpyModels(dependentSpyModels)
        return constructFieldsWithAnnotation(suitableSpyModels)
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

    private fun getSuitableSpyModels(potentialSpyModels: MutableSet<UtModelWrapper>): Set<UtModelWrapper> =
        spiedTypes.fold(setOf()) { spyModels, type ->
            spyModels + getSuitableSpyModelsOfType(type, potentialSpyModels)
        }

    /*
     * Filters out cases when different tests use different [clazz]
     * implementations and hence we need to inject different types.
     *
     * This limitation is reasoned by @InjectMocks behaviour.
     * Otherwise, injection may be misleading:
     * for example, several spies may be injected into one field.
    */
    private fun getSuitableSpyModelsOfType(
        clazz: Class<*>,
        potentialSpyModels: MutableSet<UtModelWrapper>
    ): Set<UtModelWrapper> {
        val spyModelsAssignableFrom = potentialSpyModels
            .filter { clazz.isAssignableFrom(it.model.classId.jClass) }
            .toSet()
        val spyModelsTypesCount = spyModelsAssignableFrom.map { it.model.classId }.toSet().size

        return if (spyModelsTypesCount == 1) spyModelsAssignableFrom else emptySet()
    }
}