package api

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.js.JsClassId
import org.utbot.framework.plugin.api.js.JsPrimitiveModel
import org.utbot.framework.plugin.api.js.util.jsNumberClassId
import org.utbot.framework.plugin.api.js.util.jsUndefinedClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedMethodDescriptionAdapter
import org.utbot.fuzzer.FuzzerPlatform

object JsFuzzerPlatform : FuzzerPlatform {
    override fun toPlatformClassId(otherLanguageClassId: ClassId): ClassId {
        return when (otherLanguageClassId) {
            jsNumberClassId -> intClassId
            jsUndefinedClassId -> objectClassId
            else -> super.toPlatformClassId(otherLanguageClassId)
        }
    }

    override fun toLanguageUtModel(platformUtModel: UtModel, description: FuzzedMethodDescription): UtModel {
        if (platformUtModel is UtPrimitiveModel) {
            return JsPrimitiveModel(platformUtModel.value)
        }
        return super.toLanguageUtModel(platformUtModel, description)
    }

    override fun collectConstructors(classId: ClassId, description: FuzzedMethodDescription): Sequence<ConstructorId> {
        return if (description is FuzzedMethodDescriptionAdapter) {
            (description.originClassMap[classId] as JsClassId).allConstructors
        } else {
            super.collectConstructors(classId, description)
        }
    }
}