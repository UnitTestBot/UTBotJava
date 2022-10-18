package api

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.js.JsNullModel
import org.utbot.framework.plugin.api.js.JsPrimitiveModel
import org.utbot.framework.plugin.api.js.JsUndefinedModel
import org.utbot.framework.plugin.api.js.util.jsBooleanClassId
import org.utbot.framework.plugin.api.js.util.jsDoubleClassId
import org.utbot.framework.plugin.api.js.util.jsNumberClassId
import org.utbot.framework.plugin.api.js.util.jsStringClassId
import org.utbot.framework.plugin.api.js.util.jsUndefinedClassId
import org.utbot.framework.plugin.api.js.util.toJsClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzerPlatform

object JsFuzzerPlatform : FuzzerPlatform {
    override fun toPlatformClassId(otherLanguageClassId: ClassId): ClassId {
        return when (otherLanguageClassId) {
            jsNumberClassId -> intClassId
            jsBooleanClassId -> booleanClassId
            jsDoubleClassId -> doubleClassId
            jsStringClassId -> stringClassId
            jsUndefinedClassId -> intClassId
            else -> super.toPlatformClassId(otherLanguageClassId)
        }
    }

    override fun toLanguageUtModel(platformUtModel: UtModel, description: FuzzedMethodDescription): UtModel {
        return when (platformUtModel) {
            is UtPrimitiveModel -> JsPrimitiveModel(platformUtModel.value)
            is UtNullModel -> JsNullModel(platformUtModel.classId.toJsClassId())
            is UtAssembleModel -> platformUtModel
            else -> JsUndefinedModel(platformUtModel.classId.toJsClassId())
        }
    }

    override fun collectConstructors(classId: ClassId, description: FuzzedMethodDescription) = classId.allConstructors
}