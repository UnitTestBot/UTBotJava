package org.utbot.framework.plugin.api.mapper

import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtModelWithCompositeOrigin

interface UtModelMapper {
    /**
     * Performs depending on the implementation deep or shallow mapping of the [model].
     *
     * In some cases (e.g. when mapping [UtModelWithCompositeOrigin.origin]) you may want to get result
     * of some specific type (e.g. [UtCompositeModel]), only then you should specify specific value for [clazz].
     *
     * NOTE: if you are fine with result model and [model] having different types, then you should
     * use `UtModel::class.java` as a value for [clazz] or just use [UtModel.map].
     */
    fun <T : UtModel> map(model: T, clazz: Class<T>): T
}
