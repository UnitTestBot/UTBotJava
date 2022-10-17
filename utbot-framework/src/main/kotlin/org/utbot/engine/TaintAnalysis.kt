package org.utbot.engine

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.utbot.engine.pc.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.*

class TaintAnalysis {
    // TODO move it to the Memory.kt file?


    private val taintFlagToIdBiMap: BiMap<String, Long> = HashBiMap.create()

    private fun idByFlag(flag: String): Long = taintFlagToIdBiMap.getOrPut(flag) { counter.also { counter *= 2 } }
    private fun flagById(id: Long): String = taintFlagToIdBiMap.inverse()[id] ?: error("Unknown flag id: $id")

    private var counter: Long = 1

    fun constructTaintedVector(flags: Set<String>): UtBvLiteral {
        val taintedLongValue = flags.fold(initial = 0L) { acc, flag -> acc or idByFlag(flag) }

        return UtBvLiteral(taintedLongValue, UtLongSort)
    }

    // TODO replace with a formula in the future
    fun containsInTainted(
        valueFromTaintArray: UtArraySelectExpression,
        flags: Set<String>
    ): UtBoolExpression {
        val taintedVector = valueFromTaintArray.toLongValue()
        val expectedTaintedVector = constructTaintedVector(flags).toLongValue()

        val intersection = And(taintedVector, expectedTaintedVector)
        val notContainsCondition = mkEq(intersection, mkLong(value = 0L))

        return mkNot(notContainsCondition)
    }

    /*
    * TODO Should be configurable, hardcoded for demonstration purposes
    * */
    val taintSources: Map<MethodId, Set<String>> = mapOf(
        MethodId(
            classId = java.lang.System::class.id,
            name = "getenv",
            returnType = mapClassId,
            parameters = listOf(stringClassId)
        ) to setOf(SQL_INJECTION_FLAG),
        MethodId(
            classId = ClassId("org.utbot.examples.taint.BadSource"),
            name = "getEnvironment",
            returnType = stringClassId,
            parameters = listOf(stringClassId)
        ) to setOf(SQL_INJECTION_FLAG),
    )

    val taintSinks: Map<MethodId, Set<String>> = mapOf(
        MethodId(
            classId = java.sql.Statement::class.id,
            name = "execute",
            returnType = booleanClassId,
            parameters = listOf(stringClassId)
        ) to setOf(SQL_INJECTION_FLAG),
        MethodId(
            classId = ClassId("org.utbot.examples.taint.BadSink"),
            name = "writeIntoBd",
            returnType = voidClassId,
            parameters = listOf(stringClassId)
        ) to setOf(SQL_INJECTION_FLAG),
    )

    val taintSanitizers: Map<MethodId, Set<String>> = mapOf(

    )

    val taintPassThrough: Map<MethodId, Set<String>> = mapOf(

    )

    companion object {
        const val FLAG_KINDS_NUMBER = 64 // TODO should be configurable
        const val SQL_INJECTION_FLAG = "SQL_INJECTION"
    }
}
