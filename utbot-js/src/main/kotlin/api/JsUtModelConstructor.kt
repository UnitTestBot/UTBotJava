package api

import framework.api.js.JsClassId
import framework.api.js.JsEmptyClassId
import framework.api.js.JsNullModel
import framework.api.js.JsPrimitiveModel
import framework.api.js.JsUndefinedModel
import framework.api.js.util.defaultJsValueModel
import framework.api.js.util.jsErrorClassId
import fuzzer.JsIdProvider
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructorInterface

class JsUtModelConstructor : UtModelConstructorInterface {

    // TODO SEVERE: Requires substantial expansion to other types
    @Suppress("NAME_SHADOWING")
    override fun construct(value: Any?, classId: ClassId): UtModel {
        val classId = classId as JsClassId
        if (classId == jsErrorClassId) return UtModel(jsErrorClassId)
        return when (value) {
            null -> JsNullModel(classId)
            is Byte,
            is Short,
            is Char,
            is Int,
            is Long,
            is Float,
            is Double,
            is String,
            is Boolean -> JsPrimitiveModel(value)
            is List<*> -> {
                UtArrayModel(
                    id = JsIdProvider.createId(),
                    classId = classId,
                    stores = buildMap {
                        putAll(value.indices.zip(value.map {
                            construct(it, classId)
                        }))
                    } as MutableMap<Int, UtModel>,
                    length = value.size,
                    constModel = classId.defaultJsValueModel()
                )
            }
            is Map<*, *> -> {
                constructObject(classId, value)
            }

            else -> JsUndefinedModel(classId)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun constructObject(classId: JsClassId, value: Any?): UtModel {
        val constructor = classId.allConstructors.first()
        val values = (value as Map<String, Any>).values.map {
            construct(it, JsEmptyClassId())
        }
        val id = JsIdProvider.createId()
        val instantiationCall = UtExecutableCallModel(null, constructor, values)
        return UtAssembleModel(
            id = id,
            classId = constructor.classId,
            modelName = "${constructor.classId.name}${constructor.parameters}#" + id.toString(16),
            instantiationCall = instantiationCall,
        )
    }
}
