package org.utbot.framework.codegen.domain.models.builders

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.codegen.domain.withId
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
            baseModel.classUnderTest,
            baseModel.methodTestSets,
            baseModel.nestedClasses,
            injectedModels,
            mockedModels,
        )
    }

    private fun collectInjectedAndMockedModels(
        testSets: List<CgMethodTestSet>,
    ): Pair<Map<ClassId, Set<UtModel>>, Map<ClassId, Set<UtModel>>> {
        val thisInstances = mutableSetOf<UtModel>()
        val thisInstancesDependentModels = mutableSetOf<UtModel>()

        for (testSet in testSets) {
            for ((index, execution) in testSet.executions.withIndex()) {
                execution.stateBefore.thisInstance?.let { model ->
                    thisInstances += model
                    thisInstancesDependentModels += collectByThisInstanceModel(model, index)
                }

                execution.stateAfter.thisInstance?.let { model ->
                    thisInstances += model
                    thisInstancesDependentModels += collectByThisInstanceModel(model, index)
                }
            }
        }

        val dependentMockModels =
            thisInstancesDependentModels.filterTo(mutableSetOf()) { it.isMockModel() && it !in thisInstances }

        return thisInstances.groupByClassId() to dependentMockModels.groupByClassId()
    }

    private fun collectByThisInstanceModel(model: UtModel, executionIndex: Int): Set<UtModel> {
        context.modelIds += model.withId(executionIndex)

        val dependentModels = mutableSetOf<UtModel>()
        collectRecursively(model, dependentModels)

        dependentModels.forEach { model ->
            context.modelIds += model.withId(executionIndex)
        }

        return dependentModels
    }

    private fun Set<UtModel>.groupByClassId(): Map<ClassId, Set<UtModel>> {
        return this.groupBy { it.classId }.mapValues { it.value.toSet() }
    }

    private fun collectRecursively(currentModel: UtModel, allModels: MutableSet<UtModel>) {
        if (!allModels.add(currentModel)) {
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
                currentModel.fields.values.forEach { collectRecursively(it, allModels) }
                currentModel.mocks.values.asSequence().flatten().forEach { collectRecursively(it, allModels) }
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