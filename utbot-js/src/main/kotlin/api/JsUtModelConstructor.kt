package api

import fuzzer.providers.JsObjectModelProvider
import org.utbot.framework.concrete.UtModelConstructorInterface
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.js.JsClassId
import org.utbot.framework.plugin.api.js.JsEmptyClassId
import org.utbot.framework.plugin.api.js.JsNullModel
import org.utbot.framework.plugin.api.js.JsPrimitiveModel
import org.utbot.framework.plugin.api.js.JsUndefinedModel
import org.utbot.framework.plugin.api.js.util.jsErrorClassId
import org.utbot.framework.plugin.api.js.util.jsUndefinedClassId

class JsUtModelConstructor : UtModelConstructorInterface {

    // TODO SEVERE: This is a very dirty prototype version. Expand!
    @Suppress("NAME_SHADOWING")
    override fun construct(value: Any?, classId: ClassId): UtModel {
        val classId = classId as JsClassId
        when (classId) {
            jsUndefinedClassId -> return JsUndefinedModel(classId)
            jsErrorClassId -> return UtModel(jsErrorClassId)
        }
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
        val id = JsObjectModelProvider.idGenerator.asInt
        val instantiationCall = UtExecutableCallModel(null, constructor, values)
        return UtAssembleModel(
            id = id,
            classId = constructor.classId,
            modelName = "${constructor.classId.name}${constructor.parameters}#" + id.toString(16),
            instantiationCall = instantiationCall,
        )
    }
}