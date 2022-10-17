package org.utbot.fuzzer.objects

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementModel

/**
 * Implements [MethodId] but also can supply a mock for this execution.
 *
 * Simplest example: setter and getter,
 * when this methodId is a setter, getter can be used for a mock to supply correct value.
 */
internal class FuzzerMockableMethodId(
    classId: ClassId,
    name: String,
    returnType: ClassId,
    parameters: List<ClassId>,
    val mock: () -> Map<ExecutableId, List<UtModel>> = { emptyMap() },
) : MethodId(classId, name, returnType, parameters) {

    constructor(copyOf: MethodId, mock: () -> Map<ExecutableId, List<UtModel>> = { emptyMap() }) : this(
        copyOf.classId, copyOf.name, copyOf.returnType, copyOf.parameters, mock
    )

}

internal fun MethodId.toFuzzerMockable(block: suspend SequenceScope<Pair<MethodId, List<UtModel>>>.() -> Unit): FuzzerMockableMethodId {
    return FuzzerMockableMethodId(this) {
        sequence { block() }.toMap()
    }
}

internal fun replaceWithMock(assembleModel: UtModel, shouldMock: (ClassId) -> Boolean): UtModel = when {
    assembleModel !is UtAssembleModel -> assembleModel
    shouldMock(assembleModel.classId) -> createMockModelFromFuzzerMockable(assembleModel, shouldMock)
    else -> updateInnerModels(assembleModel, shouldMock)
}

private fun createMockModelFromFuzzerMockable(model: UtAssembleModel, shouldMock: (ClassId) -> Boolean): UtCompositeModel {
    val mock = UtCompositeModel(model.id, model.classId, true)
    for (mutator in model.modificationsChain) {
        if (mutator is UtDirectSetFieldModel) {
            mock.fields[mutator.fieldId] = replaceWithMock(mutator.fieldModel, shouldMock)
        }
        if (mutator is UtExecutableCallModel && mutator.executable is FuzzerMockableMethodId) {
            (mutator.executable as FuzzerMockableMethodId).mock().forEach { (executionId, models) ->
                mock.mocks[executionId] = models.map { p -> replaceWithMock(p, shouldMock) }
            }
        }
    }
    return mock
}

private fun updateInnerModels(model: UtAssembleModel, shouldMock: (ClassId) -> Boolean): UtAssembleModel {
    val models = model.modificationsChain.map { call ->
        var mockedStatementModel: UtStatementModel? = null
        when (call) {
            is UtDirectSetFieldModel -> {
                val mock = replaceWithMock(call.fieldModel, shouldMock)
                if (mock != call.fieldModel) {
                    mockedStatementModel = UtDirectSetFieldModel(call.instance, call.fieldId, mock)
                }
            }
            is UtExecutableCallModel -> {
                val params = call.params.map { m -> replaceWithMock(m, shouldMock) }
                if (params != call.params) {
                    mockedStatementModel = UtExecutableCallModel(call.instance, call.executable, params)
                }
            }
        }
        mockedStatementModel ?: call
    }
    return with(model) {
        UtAssembleModel(id, classId, modelName, instantiationCall, origin) { models }
    }
}