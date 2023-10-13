package org.utbot.framework.plugin.api.mapper

import org.utbot.framework.plugin.api.UtModel

object UtModelNoopMapper : UtModelMapper {
    override fun <T : UtModel> map(model: T, clazz: Class<T>): T = model
}