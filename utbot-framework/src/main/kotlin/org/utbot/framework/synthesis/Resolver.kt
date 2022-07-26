package org.utbot.framework.synthesis

import org.utbot.engine.nextDefaultModelId
import org.utbot.framework.plugin.api.*
import org.utbot.framework.util.nextModelName
import java.util.IdentityHashMap

class Resolver(
    parameterModels: List<UtModel>,
    val rootUnits: List<SynthesisUnit>,
    unitToParameter: IdentityHashMap<SynthesisUnit, SynthesisParameter>,
) {
    private val unitToModel = IdentityHashMap<SynthesisUnit, UtModel>().apply {
        unitToParameter.toList().forEach { (it, parameter) -> this[it] = parameterModels[parameter.number] }

    }

    fun resolve(unit: SynthesisUnit): UtModel =
        when (unit) {
            is MethodUnit -> unitToModel.getOrPut(unit) { resolveMethodUnit(unit) }
            is ObjectUnit -> unitToModel[unit] ?: error("Can't map $unit")
            is NullUnit -> UtNullModel(unit.classId)
            is ReferenceToUnit -> resolve(rootUnits[unit.referenceParam])
            else -> TODO()
        }

    private fun resolveMethodUnit(unit: MethodUnit): UtModel =
        with(unit.method) {
            when {
                this is ConstructorId -> resolveConstructorInvoke(unit, this)
                this is MethodId && isStatic -> TODO()
                this is MethodId -> resolveVirtualInvoke(unit, this)
                else -> TODO()
            }
        }

    private fun resolveVirtualInvoke(unit: MethodUnit, method: MethodId): UtModel {
        val resolvedModels = unit.params.map { resolve(it) }

        val thisModel = resolvedModels.firstOrNull() ?: error("No this parameter found for $method")
        val modelsWithoutThis = resolvedModels.drop(1)

        if (thisModel !is UtAssembleModel) {
            error("$thisModel is not assemble")
        }

        val modificationChain = (thisModel.modificationsChain as? MutableList<UtStatementModel>)
            ?: error("Can't cast to mutable")

        modificationChain.add(
            UtExecutableCallModel(thisModel, unit.method, modelsWithoutThis)
        )

        return thisModel
    }

    private fun resolveConstructorInvoke(unit: MethodUnit, method: ConstructorId): UtModel {
        val resolvedModels = unit.params.map { resolve(it) }

        val instantiationChain = mutableListOf<UtStatementModel>()
        val modificationChain = mutableListOf<UtStatementModel>()

        val model = UtAssembleModel(
            nextDefaultModelId++,
            unit.classId,
            nextModelName("refModel_${unit.classId.simpleName}"),
            instantiationChain,
            modificationChain
        )

        instantiationChain.add(
            UtExecutableCallModel(model, unit.method, resolvedModels, model)
        )

        return model
    }
}