package org.utbot.framework.assemble

import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.*

/**
 * Creates [UtAssembleModel] of the wrapper for a given [UtPrimitiveModel].
 */
fun assemble(model: UtPrimitiveModel): UtAssembleModel {
    val modelType = model.classId
    val assembledModelType = wrapIfPrimitive(modelType)

    val constructorCall = when (modelType) {
        shortClassId -> java.lang.Short::class.java.getConstructor(Short::class.java)
        intClassId -> java.lang.Integer::class.java.getConstructor(Int::class.java)
        longClassId -> java.lang.Long::class.java.getConstructor(Long::class.java)
        charClassId -> java.lang.Character::class.java.getConstructor(Char::class.java)
        byteClassId -> java.lang.Byte::class.java.getConstructor(Byte::class.java)
        booleanClassId -> java.lang.Boolean::class.java.getConstructor(Boolean::class.java)
        floatClassId -> java.lang.Float::class.java.getConstructor(Float::class.java)
        doubleClassId -> java.lang.Double::class.java.getConstructor(Double::class.java)
        else -> error("Model type $modelType is void or non-primitive")
    }

    val constructorCallModel = UtExecutableCallModel(
        instance = null,
        executable = constructorCall.executableId,
        params = listOf(model),
        returnValue = null,
    )

    return UtAssembleModel(
        id = null,
        classId = assembledModelType,
        modelName = modelType.name,
        instantiationChain = listOf(constructorCallModel),
        modificationsChain = emptyList(),
    )
}