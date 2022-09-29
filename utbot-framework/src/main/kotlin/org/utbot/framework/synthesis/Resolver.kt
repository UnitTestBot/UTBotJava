package org.utbot.framework.synthesis

import org.utbot.engine.defaultIdGenerator
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
        when (val method = unit.method) {
            is ConstructorId -> resolveConstructorInvoke(unit, method)
            is MethodId -> resolveVirtualInvoke(unit, method)
            else -> error("Unexpected method unit in resolver: $unit")
        }

    private fun resolveVirtualInvoke(unit: MethodUnit, method: MethodId): UtModel {
        val resolvedModels = unit.params.map { resolve(it) }

        val thisModel = resolvedModels.firstOrNull() ?: error("No `this` parameter found for $method")
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

        return UtAssembleModel(
            defaultIdGenerator.createId(),
            unit.classId,
            nextModelName("refModel_${unit.classId.simpleName}"),
            UtExecutableCallModel(null, unit.method, resolvedModels),
        )
    }

    private fun resolveCollection(
        unit: ElementContainingUnit,
        constructorId: ConstructorId,
        modificationId: MethodId
    ): UtModel {
        val elements = unit.elements.map { resolve(synthesisUnitContext[it.second]) }

        return UtAssembleModel(
            defaultIdGenerator.createId(),
            unit.classId,
            nextModelName("refModel_${unit.classId.simpleName}"),
            UtExecutableCallModel(null, constructorId, listOf())
        ) {
            elements.map { value ->
                UtExecutableCallModel(
                    this, modificationId, listOf(value),
                )
            }
        }
    }

    private fun resolveList(unit: ListUnit): UtModel = resolveCollection(unit, unit.constructorId, unit.addId)

    private fun resolveSet(unit: SetUnit): UtModel = resolveCollection(unit, unit.constructorId, unit.addId)

    private fun resolveMap(unit: MapUnit): UtModel {
        val elements = unit.elements.map {
            resolve(synthesisUnitContext[it.first]) to resolve(synthesisUnitContext[it.second])
        }

        val model = UtAssembleModel(
            defaultIdGenerator.createId(),
            unit.classId,
            nextModelName("refModel_${unit.classId.simpleName}"),
            UtExecutableCallModel(null, unit.constructorId, listOf()),
        ) {
            elements.map { (key, value) ->
                UtExecutableCallModel(
                    this, unit.putId, listOf(key, value),
                )
            }
        }

        return model
    }

    private fun resolveArray(unit: ArrayUnit): UtModel {
        val lengthModel = resolve(synthesisUnitContext[unit.length]) as UtPrimitiveModel
        val elements = unit.elements.associate {
            ((resolve(synthesisUnitContext[it.first]) as UtPrimitiveModel).value as Int) to resolve(synthesisUnitContext[it.second])
        }

        return UtArrayModel(
            defaultIdGenerator.createId(),
            unit.classId,
            lengthModel.value as Int,
            unit.classId.elementClassId!!.defaultValueModel(),
            elements.toMutableMap()
        )
    }
}
