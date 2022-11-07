package api

import framework.api.ts.TsClassId
import framework.api.ts.TsEmptyClassId
import framework.api.ts.TsNullModel
import framework.api.ts.TsPrimitiveModel
import framework.api.ts.TsUndefinedModel
import framework.api.ts.util.tsErrorClassId
import framework.api.ts.util.tsUndefinedClassId
import fuzzer.providers.TsObjectModelProvider
import org.utbot.framework.concrete.UtModelConstructorInterface
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel

class TsUtModelConstructor : UtModelConstructorInterface {

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
