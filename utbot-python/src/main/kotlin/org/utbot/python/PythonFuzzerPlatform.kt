package org.utbot.python

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.python.*
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzerPlatform
import org.utbot.python.typing.PythonTypesStorage

object PythonFuzzerPlatform : FuzzerPlatform {

    override fun toPlatformClassId(otherLanguageClassId: ClassId): ClassId {
        return when (otherLanguageClassId) {
            pythonIntClassId -> intClassId
            pythonStrClassId -> stringClassId
            pythonBoolClassId -> booleanClassId
            else -> super.toPlatformClassId(otherLanguageClassId)
        }
    }

    override fun toLanguageUtModel(platformUtModel: UtModel, description: FuzzedMethodDescription): UtModel {
        if (platformUtModel is UtPrimitiveModel) {
            when (platformUtModel.classId) {
                intClassId -> return PythonPrimitiveModel(platformUtModel.value, pythonIntClassId)
                stringClassId ->
                    return PythonPrimitiveModel("\"\"\"" + platformUtModel.value + "\"\"\"", pythonStrClassId)
                booleanClassId -> return PythonBoolModel(platformUtModel.value as Boolean)
            }
        }
        if (platformUtModel is UtAssembleModel) {
            return PythonInitObjectModel(
                platformUtModel.classId.name,
                platformUtModel.instantiationCall.params.map { it as PythonModel }
            )
        }
        return super.toLanguageUtModel(platformUtModel, description)
    }

    override fun collectConstructors(classId: ClassId, description: FuzzedMethodDescription): Sequence<ConstructorId> {
        val type = PythonTypesStorage.findPythonClassIdInfoByName(classId.name) ?: return emptySequence()
        val initSignature = type.initSignature ?: return emptySequence()
        return sequenceOf(
            ConstructorId(classId, initSignature)
        )
    }
}