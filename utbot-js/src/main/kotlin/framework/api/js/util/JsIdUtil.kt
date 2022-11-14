package framework.api.js.util

import org.utbot.framework.plugin.api.ClassId
import framework.api.js.JsClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId

val jsUndefinedClassId = JsClassId("undefined")
val jsNumberClassId = JsClassId("number")
val jsBooleanClassId = JsClassId("bool")
val jsDoubleClassId = JsClassId("double")
val jsStringClassId = JsClassId("string")
val jsErrorClassId = JsClassId("error")


val jsPrimitives = setOf(
    jsNumberClassId,
    jsBooleanClassId,
    jsDoubleClassId,
)

val jsBasic = setOf(
    jsNumberClassId,
    jsBooleanClassId,
    jsDoubleClassId,
    jsUndefinedClassId,
    jsStringClassId,
)

fun ClassId.toJsClassId() =
    when {
        this == intClassId -> jsNumberClassId
        this == byteClassId -> jsNumberClassId
        this == shortClassId -> jsNumberClassId
        this == booleanClassId -> jsBooleanClassId
        this == doubleClassId -> jsDoubleClassId
        this == floatClassId -> jsDoubleClassId
        this.name.lowercase().contains("string") -> jsStringClassId
        this == longClassId -> jsNumberClassId
        else -> jsUndefinedClassId
    }

val JsClassId.isJsBasic: Boolean
    get() = this in jsBasic

val JsClassId.isJsPrimitive: Boolean
    get() = this in jsPrimitives

val JsClassId.isUndefined: Boolean
    get() = this == jsUndefinedClassId