package org.utbot.framework.codegen.domain.models.builders

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.SpringSpecificInformation
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtCustomModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext
import org.utbot.framework.plugin.api.canBeSpied

typealias TypedModelWrappers = Map<ClassId, Set<UtModelWrapper>>

class SpringTestClassModelBuilder(val context: CgContext) :
    TestClassModelBuilder(),
    CgContextOwner by context {

    override fun createTestClassModel(classUnderTest: ClassId, testSets: List<CgMethodTestSet>): SpringTestClassModel {
        val baseModel = SimpleTestClassModelBuilder(context).createTestClassModel(classUnderTest, testSets)
        val springSpecificInformation = collectSpecificModelsForClassVariables(testSets)

        return SpringTestClassModel(
            classUnderTest = baseModel.classUnderTest,
            methodTestSets = baseModel.methodTestSets,
            nestedClasses = baseModel.nestedClasses,
            springSpecificInformation = springSpecificInformation
        )
    }

    private fun collectSpecificModelsForClassVariables(testSets: List<CgMethodTestSet>): SpringSpecificInformation {
        val thisInstanceModels = mutableSetOf<UtModelWrapper>()
        val thisInstancesDependentModels = mutableSetOf<UtModelWrapper>()
        val stateBeforeDependentModels = mutableSetOf<UtModelWrapper>()

        for ((testSetIndex, testSet) in testSets.withIndex()) {
            withTestSetIdScope(testSetIndex) {
                for ((executionIndex, execution) in testSet.executions.withIndex()) {
                    withExecutionIdScope(executionIndex) {
                        setOf(execution.stateBefore.thisInstance, execution.stateAfter.thisInstance)
                            .filterNotNull()
                            .forEach { model ->
                                thisInstanceModels += model.wrap()
                                thisInstancesDependentModels += collectDependentModels(model)

                            }

                        (execution.stateBefore.parameters + execution.stateBefore.thisInstance)
                            .filterNotNull()
                            .forEach { model -> stateBeforeDependentModels += collectDependentModels(model) }
                    }
                }
            }
        }

        val dependentMockModels =
            thisInstancesDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.isMockModel() && cgModel !in thisInstanceModels
                }

        val dependentSpyModels =
            thisInstancesDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.canBeSpied() && cgModel !in thisInstanceModels && cgModel !in dependentMockModels
                }

        val autowiredFromContextModels =
            stateBeforeDependentModels.filterTo(HashSet()) { it.model.isAutowiredFromContext() }

        return SpringSpecificInformation(
            thisInstanceModels.groupByClassId(),
            dependentMockModels.groupByClassId(),
            dependentSpyModels.groupByClassId(),
            autowiredFromContextModels.groupByClassId(),
        )
    }

    private fun collectDependentModels(model: UtModel): Set<UtModelWrapper> {
        val dependentModels = mutableSetOf<UtModelWrapper>()

        when (model) {
            is UtNullModel,
            is UtPrimitiveModel,
            is UtClassRefModel,
            is UtVoidModel,
            is UtEnumConstantModel,
            is UtCustomModel -> {}
            is UtLambdaModel -> {
                model.capturedValues.forEach { dependentModels.add(it.wrap()) }
            }
            is UtArrayModel -> {
                model.stores.values.forEach { dependentModels.add(it.wrap()) }
                if (model.stores.count() < model.length) {
                    dependentModels.add(model.constModel.wrap())
                }
            }
            is UtCompositeModel -> {
                // Here we traverse fields only.
                // Traversing mocks as well will result in wrong models playing
                // a role of class fields with @Mock annotation.
                model.fields.forEach { (_, model) -> dependentModels.add(model.wrap()) }
            }
            is UtAssembleModel -> {
                model.instantiationCall.instance?.let { dependentModels.add(it.wrap()) }
                model.instantiationCall.params.forEach { dependentModels.add(it.wrap()) }
            }
        }

        return dependentModels
    }


    private fun Set<UtModelWrapper>.groupByClassId(): TypedModelWrappers {
        val classModels = mutableMapOf<ClassId, Set<UtModelWrapper>>()

        for (modelGroup in this.groupBy { it.model.classId }) {
            classModels[modelGroup.key] = modelGroup.value.toSet()
        }

        return classModels
    }

}