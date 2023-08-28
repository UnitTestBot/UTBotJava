package org.utbot.framework.plugin.api.mapper

import org.utbot.framework.plugin.api.UtModel

class UtModelSafeCastingCachingShallowMapper(
    val mapper: (UtModel) -> UtModel
) : UtModelMapper {
    private val cache = mutableMapOf<UtModel, UtModel>()

    override fun <T : UtModel> map(model: T, clazz: Class<T>): T {
        val mapped = cache.getOrPut(model) { mapper(model) }
        return if (clazz.isInstance(mapped)) clazz.cast(mapped)
        else model
    }
}