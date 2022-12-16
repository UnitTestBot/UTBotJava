package org.utbot.language.ts.api

import org.utbot.language.ts.framework.api.ts.TsClassId
import org.utbot.language.ts.framework.api.ts.TsEmptyClassId
import org.utbot.language.ts.framework.api.ts.TsNullModel
import org.utbot.language.ts.framework.api.ts.TsPrimitiveModel
import org.utbot.language.ts.framework.api.ts.TsUndefinedModel
import org.utbot.language.ts.framework.api.ts.util.tsErrorClassId
import org.utbot.language.ts.framework.api.ts.util.tsUndefinedClassId
import org.utbot.language.ts.fuzzer.providers.TsObjectModelProvider
import org.utbot.framework.concrete.UtModelConstructorInterface
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.language.ts.settings.TsDynamicSettings

class TsUtModelConstructor(settings: TsDynamicSettings) : UtModelConstructorInterface {

    // TODO SEVERE: Requires substantial expansion to other types
    @Suppress("NAME_SHADOWING")
    override fun construct(value: Any?, classId: ClassId): UtModel {
        val classId = classId as TsClassId
        when (classId) {
            tsUndefinedClassId -> return TsUndefinedModel(classId)
            tsErrorClassId -> return UtModel(tsErrorClassId)
        }
        return when (value) {
            null -> TsNullModel(classId)
            is Byte,
            is Short,
            is Char,
            is Int,
            is Long,
            is Float,
            is Double,
            is String,
            is Boolean -> TsPrimitiveModel(value)

            is Map<*, *> -> {
                constructObject(classId, value)
            }

            else -> TsUndefinedModel(classId)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun constructObject(classId: TsClassId, value: Any?): UtModel {
        val constructor = classId.allConstructors.first()
        val values = (value as Map<String, Any>).values.map {
            construct(it, TsEmptyClassId())
        }
        val id = TsObjectModelProvider.idGenerator.asInt
        val instantiationCall = UtExecutableCallModel(null, constructor, values)
        return UtAssembleModel(
            id = id,
            classId = constructor.classId,
            modelName = "${constructor.classId.name}${constructor.parameters}#" + id.toString(16),
            instantiationCall = instantiationCall,
        )
    }
}
