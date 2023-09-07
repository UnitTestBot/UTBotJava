package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtCustomModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtVoidModel

data class ModelsByOrigin(
    val thisInstanceModels: Set<UtModelWrapper>,
    val thisInstanceDependentModels: Set<UtModelWrapper>,
    val stateBeforeDependentModels: Set<UtModelWrapper>,
)

class ModelGroupsProvider(val context: CgContext): CgContextOwner by context {

    private val modelsCache = mutableMapOf<TestClassModel, ModelsByOrigin>()

    fun collectModelsByOrigin(testClassModel: TestClassModel): ModelsByOrigin {
        if (testClassModel in modelsCache) {
            return modelsCache[testClassModel]!!
        }

        val thisInstanceModels = mutableSetOf<UtModelWrapper>()
        val thisInstancesDependentModels = mutableSetOf<UtModelWrapper>()
        val stateBeforeDependentModels = mutableSetOf<UtModelWrapper>()

        for ((testSetIndex, testSet) in testClassModel.methodTestSets.withIndex()) {
            withTestSetIdScope(testSetIndex) {
                for ((executionIndex, execution) in testSet.executions.withIndex()) {
                    withExecutionIdScope(executionIndex) {
                        setOf(execution.stateBefore.thisInstance, execution.stateAfter.thisInstance)
                            .filterNotNull()
                            .forEach { model ->
                                thisInstanceModels += model.wrap()
                                thisInstancesDependentModels += collectImmediateDependentModels(
                                    model,
                                    skipModificationChains = true
                                )
                            }

                        (execution.stateBefore.parameters + execution.stateBefore.thisInstance)
                            .filterNotNull()
                            .forEach { model -> stateBeforeDependentModels += collectRecursively(model) }
                    }
                }
            }
        }

        val modelsByOrigin = ModelsByOrigin(thisInstanceModels, thisInstancesDependentModels, stateBeforeDependentModels)
        modelsCache[testClassModel] = modelsByOrigin

        return modelsByOrigin
    }

    private fun collectImmediateDependentModels(model: UtModel, skipModificationChains: Boolean): Set<UtModelWrapper> {
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

                if(!skipModificationChains) {
                    model.modificationsChain.forEach { stmt ->
                        stmt.instance?.let { dependentModels.add(it.wrap()) }
                        when (stmt) {
                            is UtStatementCallModel -> stmt.params.forEach { dependentModels.add(it.wrap()) }
                            is UtDirectSetFieldModel -> dependentModels.add(stmt.fieldModel.wrap())
                        }
                    }
                }
            }
        }

        return dependentModels
    }

    private fun collectRecursively(model: UtModel): Set<UtModelWrapper> {
        val allDependentModels = mutableSetOf<UtModelWrapper>()

        collectRecursively(model, allDependentModels)

        return allDependentModels
    }

    private fun collectRecursively(model: UtModel, allDependentModels: MutableSet<UtModelWrapper>){
        if(!allDependentModels.add(model.wrap())){
            return
        }
        collectImmediateDependentModels(model, skipModificationChains = false).forEach {
            collectRecursively(it.model, allDependentModels)
        }
    }
}