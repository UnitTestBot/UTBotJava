package org.utbot.framework.plugin.api.mapper

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
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtVoidModel
import java.util.IdentityHashMap

/**
 * Performs deep mapping of [UtModel]s.
 *
 * NOTE:
 *  - [shallowMapper] is invoked on models **before** mapping their sub models.
 *  - [shallowMapper] is responsible for caching own results (it may be called repeatedly on same models).
 */
class UtModelDeepMapper private constructor(
    private val shallowMapper: UtModelMapper
) : UtModelMapper {
    constructor(shallowMapper: (UtModel) -> UtModel) : this(UtModelSafeCastingCachingShallowMapper(shallowMapper))

    /**
     * Keys are models that have been shallowly mapped by [shallowMapper].
     * Values are models that have been deeply mapped by this [UtModelDeepMapper].
     * Models are only associated with models of the same type (i.e. the cache type is actually `MutableMap<T, T>`)
     */
    private val cache = IdentityHashMap<UtModel, UtModel>()

    private val allInputtedModels get() = cache.keys
    private val allOutputtedModels get() = cache.values

    override fun <T : UtModel> map(model: T, clazz: Class<T>): T =
        clazz.cast(mapNestedModels(shallowMapper.map(model, clazz)))

    /**
     * Maps models contained inside [model], but not the [model] itself.
     */
    private fun mapNestedModels(model: UtModel): UtModel = cache.getOrPut(model) {
        when (model) {
            is UtNullModel,
            is UtPrimitiveModel,
            is UtEnumConstantModel,
            is UtClassRefModel,
            is UtVoidModel -> model
            is UtArrayModel -> mapNestedModels(model)
            is UtCompositeModel -> mapNestedModels(model)
            is UtLambdaModel -> mapNestedModels(model)
            is UtAssembleModel -> mapNestedModels(model)
            is UtCustomModel -> mapNestedModels(model)

            // PythonModel, JsUtModel may be here
            else -> throw UnsupportedOperationException("UtModel $this cannot be mapped")
        }
    }

    private fun mapNestedModels(model: UtArrayModel): UtReferenceModel {
        val mappedModel = UtArrayModel(
            id = model.id,
            classId = model.classId,
            length = model.length,
            constModel = model.constModel,
            stores = model.stores,
        )
        cache[model] = mappedModel

        mappedModel.constModel = model.constModel.map(this)
        mappedModel.stores.putAll(model.stores.mapModelValues(this))

        return mappedModel
    }

    private fun mapNestedModels(model: UtCompositeModel): UtCompositeModel {
        val mappedModel = UtCompositeModel(
            id = model.id,
            classId = model.classId,
            isMock = model.isMock,
        )
        cache[model] = mappedModel

        mappedModel.fields.putAll(model.fields.mapModelValues(this))
        mappedModel.mocks.putAll(model.mocks.mapValuesTo(mutableMapOf()) { it.value.mapModels(this@UtModelDeepMapper) })

        return mappedModel
    }

    private fun mapNestedModels(model: UtLambdaModel): UtReferenceModel = UtLambdaModel(
        id = model.id,
        samType = model.samType,
        declaringClass = model.declaringClass,
        lambdaName = model.lambdaName,
        capturedValues = model.capturedValues.mapModels(this@UtModelDeepMapper).toMutableList()
    )

    private fun mapNestedModels(model: UtAssembleModel): UtReferenceModel = UtAssembleModel(
        id = model.id,
        classId = model.classId,
        modelName = model.modelName,
        instantiationCall = model.instantiationCall.mapModels(this),
        modificationsChainProvider = {
            cache[model] = this@UtAssembleModel
            model.modificationsChain.map { it.mapModels(this@UtModelDeepMapper) }
        },
        origin = model.origin?.mapPreservingType<UtCompositeModel>(this)
    )

    private fun mapNestedModels(model: UtCustomModel): UtReferenceModel =
        model.shallowMap(this)

    companion object {
        /**
         * Creates identity deep mapper, runs [block] on it, and returns the set of all models that
         * were mapped (i.e. deeply collects all models reachable from models passed to `collector`).
         */
        fun collectAllModels(block: (collector: UtModelDeepMapper) -> Unit): Set<UtModel> =
            UtModelDeepMapper(UtModelNoopMapper).also(block).allInputtedModels
    }
}
