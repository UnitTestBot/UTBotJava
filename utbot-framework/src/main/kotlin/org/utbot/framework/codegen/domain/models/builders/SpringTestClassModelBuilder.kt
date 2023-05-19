package org.utbot.framework.codegen.domain.models.builders

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.isMockModel

class SpringTestClassModelBuilder(val context: CgContext): TestClassModelBuilder() {

    override fun createTestClassModel(classUnderTest: ClassId, testSets: List<CgMethodTestSet>): SpringTestClassModel {
        val baseModel = SimpleTestClassModelBuilder(context).createTestClassModel(classUnderTest, testSets)
        val (injectedModels, mockedModels) = collectInjectedAndMockedModels(testSets)

        return SpringTestClassModel(
            classUnderTest = baseModel.classUnderTest,
            methodTestSets = baseModel.methodTestSets,
            nestedClasses = baseModel.nestedClasses,
            injectedMockModels = injectedModels,
            mockedModels = mockedModels
        )
    }

    private fun collectInjectedAndMockedModels(testSets: List<CgMethodTestSet>): Pair<Map<ClassId, Set<UtModelWrapper>>, Map<ClassId, Set<UtModelWrapper>>> {
        val thisInstances = mutableSetOf<UtModelWrapper>()
        val thisInstancesDependentModels = mutableSetOf<UtModelWrapper>()

        with(context) {
            for ((testSetIndex, testSet) in testSets.withIndex()) {
                withTestSetIdScope(testSetIndex) {
                    for ((executionIndex, execution) in testSet.executions.withIndex()) {
                        withExecutionIdScope(executionIndex) {
                            setOf(execution.stateBefore.thisInstance, execution.stateAfter.thisInstance)
                                .filterNotNull()
                                .forEach { model ->
                                    thisInstances += model.wrap()
                                    thisInstancesDependentModels += collectByThisInstanceModel(model)
                                }
                        }
                    }
                }
            }
        }

        val dependentMockModels =
            thisInstancesDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.isMockModel() && cgModel !in thisInstances
                }

        return thisInstances.groupByClassId() to dependentMockModels.groupByClassId()
    }

    private fun collectByThisInstanceModel(model: UtModel): Set<UtModelWrapper> {
        val dependentModels = mutableSetOf<UtModelWrapper>()
        collectRecursively(model, dependentModels)

        return dependentModels
    }

    private fun Set<UtModelWrapper>.groupByClassId(): Map<ClassId, Set<UtModelWrapper>> {
        val classModels = mutableMapOf<ClassId, Set<UtModelWrapper>>()

        for (modelGroup in this.groupBy { it.model.classId }) {
            classModels[modelGroup.key] = modelGroup.value.toSet()
        }

        return classModels
    }

    private fun collectRecursively(currentModel: UtModel, allModels: MutableSet<UtModelWrapper>) {
        val cgModel = with(context) { currentModel.wrap() }
        if (!allModels.add(cgModel)) {
            return
        }

        when (currentModel) {
            is UtNullModel,
            is UtPrimitiveModel,
            is UtClassRefModel,
            is UtVoidModel,
            is UtEnumConstantModel -> {}
            is UtLambdaModel -> {
                currentModel.capturedValues.forEach { collectRecursively(it, allModels) }
            }
            is UtArrayModel -> {
                currentModel.stores.values.forEach { collectRecursively(it, allModels) }
                if (currentModel.stores.count() < currentModel.length) {
                    collectRecursively(currentModel.constModel, allModels)
                }
            }
            is UtCompositeModel -> {
                // Here we traverse fields only.
                // Traversing mocks as well will result in wrong models playing
                // a role of class fields with @Mock annotation.
                currentModel.fields.values.forEach { collectRecursively(it, allModels) }
            }
            is UtAssembleModel -> {
                currentModel.origin?.let { collectRecursively(it, allModels) }

                currentModel.instantiationCall.instance?.let { collectRecursively(it, allModels) }
                currentModel.instantiationCall.params.forEach { collectRecursively(it, allModels) }

                currentModel.modificationsChain.forEach { stmt ->
                    stmt.instance?.let { collectRecursively(it, allModels) }
                    when (stmt) {
                        is UtExecutableCallModel -> stmt.params.forEach { collectRecursively(it, allModels) }
                        is UtDirectSetFieldModel -> collectRecursively(stmt.fieldModel, allModels)
                    }
                }
            }
            //Python, JavaScript, Go models are not required in Spring
        }
    }
}