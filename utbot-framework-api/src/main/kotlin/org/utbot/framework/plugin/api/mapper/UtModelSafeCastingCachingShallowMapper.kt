package org.utbot.framework.plugin.api.mapper

import org.utbot.framework.plugin.api.UtModel
import java.util.IdentityHashMap

class UtModelSafeCastingCachingShallowMapper(
    val mapper: (UtModel) -> UtModel
) : UtModelMapper {
    private val cache = IdentityHashMap<UtModel, UtModel>()

    override fun <T : UtModel> map(model: T, clazz: Class<T>): T {
        val mapped = cache.getOrPut(model) { mapper(model) }
        return if (clazz.isInstance(mapped)) clazz.cast(mapped)
        else model
    }
}