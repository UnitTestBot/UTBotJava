package org.utbot.framework.codegen.domain.models

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
import org.utbot.framework.plugin.api.util.enclosingClass

class TestClassModelBuilder(
    private val isSpringClass: Boolean
) {
    fun createClassModel(classUnderTest: ClassId, testSets: List<CgMethodTestSet>): TestClassModel {
        // For each class stores list of methods declared in this class (methods from nested classes are excluded)
        val class2methodTestSets = testSets.groupBy { it.executableId.classId }

        val classesWithMethodsUnderTest = testSets
            .map { it.executableId.classId }
            .distinct()

        // For each class stores list of its "direct" nested classes
        val class2nestedClasses = mutableMapOf<ClassId, MutableSet<ClassId>>()

        for (classId in classesWithMethodsUnderTest) {
            var currentClass = classId
            var enclosingClass = currentClass.enclosingClass
            // while we haven't reached the top of nested class hierarchy or the main class under test
            while (enclosingClass != null && currentClass != classUnderTest) {
                class2nestedClasses.getOrPut(enclosingClass) { mutableSetOf() } += currentClass
                currentClass = enclosingClass
                enclosingClass = enclosingClass.enclosingClass
            }
        }

        val baseModel = constructRecursively(classUnderTest, class2methodTestSets, class2nestedClasses)

        return if (!isSpringClass) {
            baseModel
        } else {
            val mockedClasses = collectMockedClassIds(classUnderTest, testSets)

            TestClassModel(
                baseModel.classUnderTest,
                baseModel.methodTestSets,
                baseModel.nestedClasses,
                classUnderTest,
                mockedClasses,
            )
        }
    }

    private fun constructRecursively(
        clazz: ClassId,
        class2methodTestSets: Map<ClassId, List<CgMethodTestSet>>,
        class2nestedClasses: Map<ClassId, Set<ClassId>>
    ): TestClassModel {
        val currentNestedClasses = class2nestedClasses.getOrDefault(clazz, listOf())
        val currentMethodTestSets = class2methodTestSets.getOrDefault(clazz, listOf())
        return TestClassModel(
            clazz,
            currentMethodTestSets,
            currentNestedClasses.map {
                constructRecursively(it, class2methodTestSets, class2nestedClasses)
            }
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
            }
        }

        val allConstructedModels = HashSet<UtModel>()
        allModelsInExecution.forEach { model -> collectRecursively(model, allConstructedModels) }

        return allConstructedModels
            .filter { it.isMockComposite() || it.isMockAssemble() }
            .map { it.classId }
            .filter {  it != classUnderTest }
            .toSet()

    }

    private fun collectRecursively(currentModel: UtModel, allModels: HashSet<UtModel>) {
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
                collectRecursively(currentModel.constModel, allModels)
                currentModel.stores.values.forEach { collectRecursively(it, allModels) }
            }
            is UtCompositeModel -> {
                currentModel.fields.values.forEach { collectRecursively(it, allModels) }
                currentModel.mocks.values.flatten().forEach { collectRecursively(it, allModels) }
            }
            is UtAssembleModel -> {
                if (currentModel.origin != null) {
                    collectRecursively(currentModel.origin!!, allModels)
                } else {
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
            }
            //Python, JavaScript, Go models are not required in Spring
            else -> {}
        }
    }

    private fun UtModel.isMockComposite(): Boolean = this is UtCompositeModel && this.isMock

    private fun UtModel.isMockAssemble(): Boolean =
        this is UtAssembleModel && this.origin?.let { it.isMock } == true
}