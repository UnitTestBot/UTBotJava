package org.utbot.framework.synthesis

import org.utbot.engine.nextDefaultModelId
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.util.nextModelName
import java.util.IdentityHashMap

class Resolver(
    parameterModels: List<UtModel>,
    val synthesisUnitContext: SynthesisUnitContext,
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
            is ReferenceToUnit -> resolve(synthesisUnitContext[unit.reference])
            is ArrayUnit -> unitToModel.getOrPut(unit) { resolveArray(unit) }
            is ListUnit -> unitToModel.getOrPut(unit) { resolveList(unit) }
            is SetUnit -> unitToModel.getOrPut(unit) { resolveSet(unit) }
            is MapUnit -> unitToModel.getOrPut(unit) { resolveMap(unit) }
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

    private fun resolveCollection(
        unit: ElementContainingUnit,
        constructorId: ConstructorId,
        modificationId: MethodId
    ): UtModel {
        val elements = unit.elements.map { resolve(synthesisUnitContext[it.second]) }

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
            UtExecutableCallModel(model, constructorId, listOf(), model)
        )

        for (value in elements) {
            modificationChain.add(
                UtExecutableCallModel(
                    model, modificationId, listOf(value),
                )
            )
        }

        return model
    }

    private fun resolveList(unit: ListUnit): UtModel = resolveCollection(unit, unit.constructorId, unit.addId)

    private fun resolveSet(unit: SetUnit): UtModel = resolveCollection(unit, unit.constructorId, unit.addId)

    private fun resolveMap(unit: MapUnit): UtModel {
        val elements = unit.elements.map {
            resolve(synthesisUnitContext[it.first]) to resolve(synthesisUnitContext[it.second])
        }

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
            UtExecutableCallModel(model, unit.constructorId, listOf(), model)
        )

        for ((key, value) in elements) {
            modificationChain.add(
                UtExecutableCallModel(
                    model, unit.putId, listOf(key, value),
                )
            )
        }

        return model
    }

    private fun resolveArray(unit: ArrayUnit): UtModel {
        val lengthModel = resolve(synthesisUnitContext[unit.length]) as UtPrimitiveModel
        val elements = unit.elements.associate {
            ((resolve(synthesisUnitContext[it.first]) as UtPrimitiveModel).value as Int) to resolve(synthesisUnitContext[it.second])
        }

        return UtArrayModel(
            nextDefaultModelId++,
            unit.classId,
            lengthModel.value as Int,
            unit.classId.elementClassId!!.defaultValueModel(),
            elements.toMutableMap()
        )
    }
}