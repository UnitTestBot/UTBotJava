package org.utbot.framework.codegen.domain.models.builders

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
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtVoidModel

class SpringTestClassModelBuilder: TestClassModelBuilder() {

    override fun createTestClassModel(classUnderTest: ClassId, testSets: List<CgMethodTestSet>): SpringTestClassModel {
        val baseModel = SimpleTestClassModelBuilder().createTestClassModel(classUnderTest, testSets)
        val mockedClasses = collectMockedClassIds(classUnderTest, testSets)

        return SpringTestClassModel(
            baseModel.classUnderTest,
            baseModel.methodTestSets,
            baseModel.nestedClasses,
            classUnderTest,
            mockedClasses,
        )
    }

    private fun collectMockedClassIds(
        classUnderTest: ClassId,
        testSets: List<CgMethodTestSet>,
    ): Set<ClassId> {
        val allModelsInExecution = mutableListOf<UtModel>()

        for (testSet in testSets) {
            for (execution in testSet.executions) {
                execution.stateBefore.thisInstance?.let { allModelsInExecution += it }
                execution.stateAfter.thisInstance?.let { allModelsInExecution += it }

                allModelsInExecution += execution.stateBefore.parameters
                allModelsInExecution += execution.stateAfter.parameters

                (execution.result as? UtExecutionSuccess)?.model?.let { allModelsInExecution += it }
            }
        }

        val allConstructedModels = mutableSetOf<UtModel>()
        allModelsInExecution.forEach { model -> collectRecursively(model, allConstructedModels) }

        return allConstructedModels
            .filter { it.isMockComposite() || it.isMockAssemble() }
            .map { it.classId }
            .filter {  it != classUnderTest }
            .toSet()

    }

    private fun collectRecursively(currentModel: UtModel, allModels: MutableSet<UtModel>) {
        if (currentModel in allModels) {
            return
        }

        allModels += currentModel

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

    private fun UtModel.isMockComposite(): Boolean = this is UtCompositeModel && this.isMock

    //TODO: Having an assemble model often means that we do not use its origin, so is this composite mock redundant?
    private fun UtModel.isMockAssemble(): Boolean = this is UtAssembleModel && this.origin?.isMock == true
}