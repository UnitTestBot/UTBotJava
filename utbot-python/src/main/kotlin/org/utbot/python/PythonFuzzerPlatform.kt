package org.utbot.python

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.python.PythonPrimitiveModel
import org.utbot.framework.plugin.api.python.pythonIntClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzerPlatform

object PythonFuzzerPlatform : FuzzerPlatform {

    override fun toPlatformClassId(otherLanguageClassId: ClassId): ClassId {
        return when (otherLanguageClassId) {
            pythonIntClassId -> intClassId
            else -> super.toPlatformClassId(otherLanguageClassId)
        }
    }

    override fun toLanguageUtModel(platformUtModel: UtModel, description: FuzzedMethodDescription): UtModel {
        if (platformUtModel is UtPrimitiveModel) {
            return PythonPrimitiveModel(platformUtModel.value, pythonIntClassId)
        }
        return super.toLanguageUtModel(platformUtModel, description)
    }
}