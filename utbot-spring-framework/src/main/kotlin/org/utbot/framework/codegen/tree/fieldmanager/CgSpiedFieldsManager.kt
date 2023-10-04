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

        val suitableDependentSpyModels = getSuitableDependentSpyModels(dependentSpyModels)
        return constructFieldsWithAnnotation(suitableDependentSpyModels)
    }

    /*
     * If we have models of different types implementing Collection,
     * we should not construct fields of these models with @Spy annotation
     * because in this case, Spring cannot inject fields.
     *
     * The situation is similar with Map.
     */
    private fun getSuitableDependentSpyModels(dependentSpyModels: MutableSet<UtModelWrapper>): Set<UtModelWrapper> =
        getSuitableDependentSpyModelsImplementing(Collection::class.java, dependentSpyModels) +
                getSuitableDependentSpyModelsImplementing(Map::class.java, dependentSpyModels)


    private fun getSuitableDependentSpyModelsImplementing(clazz: Class<*>, dependentSpyModels: MutableSet<UtModelWrapper>): Set<UtModelWrapper> {
        return when{
            isSuitableSpyModelsImplementing(clazz, dependentSpyModels) -> dependentSpyModels.filter { clazz.isAssignableFrom(it.model.classId.jClass) }.toSet()
            else -> emptySet()
        }
    }

    /*
    * Models implementing Collection will be suitable if they are all the same type.
    *
    * The situation is similar with Map.
    */
    private fun isSuitableSpyModelsImplementing(clazz: Class<*>, dependentSpyModels: MutableSet<UtModelWrapper>): Boolean {
        val modelsClassIdsSet = HashSet<ClassId>()
        dependentSpyModels.forEach {
            val modelClassId = it.model.classId
            if(clazz.isAssignableFrom(modelClassId.jClass)){
                modelsClassIdsSet.add(modelClassId)
            }
        }

        return modelsClassIdsSet.size == 1
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