package framework.api.ts.util

import framework.api.ts.TsClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId

val tsUndefinedClassId = TsClassId("undefined")
val tsNumberClassId = TsClassId("numberkeyword")
val tsBooleanClassId = TsClassId("bool")
val tsDoubleClassId = TsClassId("double")
val tsStringClassId = TsClassId("string")
val tsErrorClassId = TsClassId("error")


val tsPrimitives = setOf(
    tsNumberClassId,
    tsBooleanClassId,
    tsDoubleClassId,
)

val tsBasic = setOf(
    tsNumberClassId,
    tsBooleanClassId,
    tsDoubleClassId,
    tsUndefinedClassId,
    tsStringClassId,
)

fun ClassId.toTsClassId() =
    when {
        this == intClassId -> tsNumberClassId
        this == byteClassId -> tsNumberClassId
        this == shortClassId -> tsNumberClassId
        this == booleanClassId -> tsBooleanClassId
        this == doubleClassId -> tsDoubleClassId
        this == floatClassId -> tsDoubleClassId
        this.name.lowercase().contains("string") -> tsStringClassId
        this == longClassId -> tsNumberClassId
        else -> tsUndefinedClassId
    }

val TsClassId.isTsBasic: Boolean
    get() = this in tsBasic

val TsClassId.isTsPrimitive: Boolean
    get() = this in tsPrimitives

val TsClassId.isUndefined: Boolean
    get() = this == tsUndefinedClassId