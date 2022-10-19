package org.utbot.engine

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.utbot.engine.pc.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
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
    val taintSources: Map<MethodId, ParamIndexToTaintFlags> = mapOf(
        MethodId(
            classId = java.lang.System::class.id,
            name = "getenv",
            returnType = stringClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
        MethodId(
            classId = ClassId("org.utbot.examples.taint.BadSource"),
            name = "getEnvironment",
            returnType = stringClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
    )

    val taintSinks: Map<MethodId, ParamIndexToTaintFlags> = mapOf(
        MethodId(
            classId = java.sql.Statement::class.id,
            name = "execute",
            returnType = booleanClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(1 to setOf(SQL_INJECTION_FLAG)),
        MethodId(
            classId = ClassId("org.utbot.examples.taint.BadSink"),
            name = "writeIntoBd",
            returnType = voidClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(1 to setOf(SQL_INJECTION_FLAG)),
        MethodId(
            classId = ClassId("org.utbot.examples.taint.BadSink"),
            name = "onlySecondParamIsImportant",
            returnType = voidClassId,
            parameters = listOf(stringClassId, stringClassId)
        ) to mapOf(2 to setOf(SQL_INJECTION_FLAG)),
    )

    val taintSanitizers: Map<MethodId, ParamIndexToTaintFlags> = mapOf(
        MethodId(
            classId = ClassId("org.utbot.examples.taint.TaintCleaner"),
            name = "removeTaintMark",
            returnType = stringClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(1 to setOf(SQL_INJECTION_FLAG)),
    )

    // int here is about from what parameter should we transfer a flag to the result value of the method
    val taintPassThrough: Map<MethodId, PassThroughArgument> = mapOf(
        MethodId(
            classId = stringBuilderClassId,
            name = "append",
            returnType = stringBuilderClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(THIS_PARAM_INDEX to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringBuilderClassId,
            name = "toString",
            returnType = stringClassId,
            parameters = emptyList()
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = ClassId("org.utbot.examples.taint.TaintPassThrough"),
            name = "passThroughTaintInformation",
            returnType = stringClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(1 to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = ClassId("org.utbot.examples.taint.TaintPassThrough"),
            name = "passSecondParameter",
            returnType = stringClassId,
            parameters = listOf(stringClassId, stringClassId)
        ) to mapOf(2 to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = ClassId("org.utbot.examples.taint.TaintPassThrough"),
            name = "passFirstParameter",
            returnType = stringClassId,
            parameters = listOf(stringClassId, stringClassId)
        ) to mapOf(1 to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
    )

    companion object {
        const val FLAG_KINDS_NUMBER = 64 // TODO should be configurable
        const val SQL_INJECTION_FLAG = "SQL_INJECTION"

        // to mark index of returned value
        const val RETURN_VALUE_INDEX = -1
        const val THIS_PARAM_INDEX = 0
    }
}

/**
 * An artificial error that could be implicitly thrown by the symbolic engine during taint sink processing.
 *
 * The [sinkSourcePosition] is a position of the [taintSink] from the source ode received from Soot.
 * Since the source code is not always available, [sinkSourcePosition] could be null.
 *
 * NOTE: should be inherited from [Error] to not be caught with `catch (e: Exception)`.
 */
class TaintAnalysisError(message: String, val taintSink: ExecutableId, val sinkSourcePosition: Int? = null) : Error(message)

typealias ParamIndexToTaintFlags = Map<Int, Set<String>>
private typealias SourceIndex = Int
private typealias TargetIndex = Int
typealias PassThroughArgument = Map<SourceIndex, Pair<TargetIndex, Set<String>>>