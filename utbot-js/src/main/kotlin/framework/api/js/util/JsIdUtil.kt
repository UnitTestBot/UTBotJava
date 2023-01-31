package framework.api.js.util

import framework.api.js.JsClassId
import org.utbot.framework.plugin.api.ClassId
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

val JsClassId.isJsBasic: Boolean
    get() = this in jsBasic

val JsClassId.isUndefined: Boolean
    get() = this == jsUndefinedClassId