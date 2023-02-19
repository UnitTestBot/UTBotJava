package framework.api.js.util

import framework.api.js.JsClassId
import framework.api.js.JsMultipleClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId

val jsUndefinedClassId = JsClassId("undefined")
val jsNumberClassId = JsClassId("number")
val jsBooleanClassId = JsClassId("bool")
val jsDoubleClassId = JsClassId("double")
val jsStringClassId = JsClassId("string")
val jsErrorClassId = JsClassId("error")

val jsBasic = setOf(
    jsBooleanClassId,
    jsDoubleClassId,
    jsUndefinedClassId,
    jsStringClassId,
    jsNumberClassId
)

fun ClassId.toJsClassId() =
    when {
        this == booleanClassId -> jsBooleanClassId
        this == doubleClassId -> jsDoubleClassId
        this == floatClassId -> jsDoubleClassId
        this.name.lowercase().contains("string") -> jsStringClassId
        else -> jsUndefinedClassId
    }

fun JsClassId.defaultJsValueModel(): UtModel = when (this) {
    jsNumberClassId -> UtPrimitiveModel(0.0)
    jsDoubleClassId -> UtPrimitiveModel(Double.POSITIVE_INFINITY)
    jsBooleanClassId -> UtPrimitiveModel(false)
    jsStringClassId -> UtPrimitiveModel("default")
    jsUndefinedClassId -> UtPrimitiveModel(0.0)
    else -> UtNullModel(this)
}

val JsClassId.isJsBasic: Boolean
    get() = this in jsBasic || this is JsMultipleClassId

val JsClassId.isExportable: Boolean
    get() = !(this.isJsBasic || this == jsErrorClassId || this.isJsArray)

val JsClassId.isClass: Boolean
    get() = !(this.isJsBasic || this == jsErrorClassId)

val JsClassId.isUndefined: Boolean
    get() = this == jsUndefinedClassId

val JsClassId.isJsArray: Boolean
    get() = this.name == "array" && this.elementClassId is JsClassId