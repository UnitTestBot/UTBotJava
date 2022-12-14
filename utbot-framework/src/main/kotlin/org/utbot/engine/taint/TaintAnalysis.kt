package org.utbot.engine.taint

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.utbot.engine.And
import org.utbot.engine.pc.*
import org.utbot.engine.toLongValue
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.*
import soot.jimple.Stmt

class TaintAnalysis {
    // TODO move it to the Memory.kt file?


    private val taintFlagToIdBiMap: BiMap<String, Long> = HashBiMap.create()

    internal fun idByFlag(flag: String): Long = taintFlagToIdBiMap.getOrPut(flag) { counter.also { counter *= 2 } }
    internal fun containsFlag(flag: String): Boolean = flag in taintFlagToIdBiMap
    private fun flagById(id: Long): String = taintFlagToIdBiMap.inverse()[id] ?: error("Unknown flag id: $id")

    private var counter: Long = 1

    // TODO public mutable - bad design
    // TODO we cannot get a value by regex key, iteration is required - perhaps traverse the whole query first and make these maps with ExecutableId as keys?
    val sources: MutableMap<Regex, MutableList<TaintSource>> = mutableMapOf()
    val knownSourceStatements: MutableMap<Stmt, TaintKinds> = mutableMapOf()
    val sinks: MutableMap<Regex, MutableList<TaintSink>> = mutableMapOf()
    val passThrough: MutableMap<Regex, MutableList<TaintPassThrough>> = mutableMapOf()
    val sanitizers: MutableMap<Regex, MutableList<TaintSanitizer>> = mutableMapOf()

    val ExecutableId.fullName: String
        get() {
            return kotlin.runCatching {
//                val packageName = classId.packageName
                val className = classId.name
                val executableName = name

                "$className.$executableName"
            }.getOrDefault("")
        }

    // TODO maps
    fun getSourceInfo(source: ExecutableId): List<TaintSource> =
        sources.filter { it.key.matches(source.fullName) }.entries.flatMap { it.value }

    fun getTaintKindsByStatement(stmt: Stmt): TaintKinds? = knownSourceStatements[stmt]

    fun getSinkInfo(sink: ExecutableId): List<TaintSink> =
        // TODO not first, but union of all sinks or list of them
        sinks.filter { it.key.matches(sink.fullName) }.entries.flatMap { it.value }

    fun getPassThroughInfo(executableId: ExecutableId): List<TaintPassThrough> =
        passThrough.filter { it.key.matches(executableId.fullName) }.entries.flatMap { it.value }

    fun getSanitizerInfo(executableId: ExecutableId): List<TaintSanitizer> =
        sanitizers.filter { it.key.matches(executableId.fullName) }.entries.flatMap { it.value }

    // TODO what to do with negation of kind?
    fun constructTaintedVector(flags: Set<String>): UtBvLiteral {
        if (flags.isEmpty()) {
            return UtBvLiteral(
                -1L,
                UtLongSort
            ) // TODO is it right? If there is no flag information, each flag is an option
        }

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

    fun setConfiguration(taintConfiguration: TaintConfiguration) {
        val (
            sourceConfigurations,
            entryPointsConfigurations,
            sinkConfigurations,
            passThroughConfigurations,
            sanitizerConfigurations
        ) = taintConfiguration

        setSources(sourceConfigurations)
        setSinks(sinkConfigurations)
        setPathTrough(passThroughConfigurations)
        setSanitizers(sanitizerConfigurations)
    }

    fun addKnownSourceStatements(sourceStatements: Map<Stmt, TaintKinds>) {
        knownSourceStatements += sourceStatements
    }

    private fun setSources(sourceConfigurations: Collection<TaintSourceConfiguration>) {
        sourceConfigurations.forEach {
            val pattern = it.pattern
            sources.putIfAbsent(pattern, mutableListOf())
            sources.getValue(pattern) += TaintSource(pattern, it.taintOutput.toSet(), it.taintKinds, it.condition)
        }
    }

    private fun setEntryPoints(entryPointConfigurations: Collection<TaintEntryPointConfiguration>) {
        entryPointConfigurations.forEach {  }
    }

    private fun setSinks(sinkConfigurations: Collection<TaintSinkConfiguration>) {
        sinkConfigurations.forEach { sinkConfiguration ->
            val pattern = sinkConfiguration.pattern

            val taintSinks = sinkConfiguration.sinks.associate { it.index to it.condition }
            sinks.putIfAbsent(pattern, mutableListOf())
            sinks.getValue(pattern) += TaintSink(pattern, taintSinks)
        }
    }

    private fun setPathTrough(passThroughConfigurations: Collection<TaintPassThroughConfiguration>) {
        passThroughConfigurations.forEach {
            val pattern = it.pattern

            passThrough.putIfAbsent(pattern, mutableListOf())
            passThrough.getValue(pattern) += TaintPassThrough(
                pattern,
                it.taintInput.toSet(),
                it.taintOutput.toSet(),
                it.taintKinds,
                it.condition
            )
        }

        taintPassThrough.forEach { (method, arg) ->
            val pattern = "${method.classId.name}.${method.name}".toRegex()

            passThrough.putIfAbsent(pattern, mutableListOf())
            passThrough.getValue(pattern) += TaintPassThrough(
                pattern, arg.keys, arg.values.map { it.first }.toSet(), TaintKinds(), condition = null
            )
        }
    }

    private fun setSanitizers(sanitizerConfigurations: Collection<TaintSanitizerConfiguration>) {
        sanitizerConfigurations.forEach {
            val pattern = it.pattern

            sanitizers.putIfAbsent(pattern, mutableListOf())
            sanitizers.getValue(pattern) += TaintSanitizer(
                pattern,
                it.taintOutput.toSet(),
                it.taintKinds
            )
        }
    }

    /*
    * TODO Should be configurable, hardcoded for demonstration purposes
    * */
    val taintSources: Map<ExecutableId, ParamIndexToTaintFlags> = mapOf(
        MethodId(
            classId = systemCLassId,
            name = "getenv",
            returnType = stringClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
        MethodId(
            classId = propertiesClassId,
            name = "getProperty",
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

    val taintSinks: Map<ExecutableId, ParamIndexToTaintFlags> = mapOf(
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

    val taintSanitizers: Map<ExecutableId, ParamIndexToTaintFlags> = mapOf(
        MethodId(
            classId = ClassId("org.utbot.examples.taint.TaintCleaner"),
            name = "removeTaintMark",
            returnType = stringClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(1 to setOf(SQL_INJECTION_FLAG)),
    )

    // int here is about from what parameter should we transfer a flag to the result value of the method
    val taintPassThrough: Map<ExecutableId, PassThroughArgument> = mapOf(
        MethodId(
            classId = stringBuilderClassId,
            name = "append",
            returnType = stringBuilderClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(
            THIS_PARAM_INDEX to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG)),
            1 to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG)),
            THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
            1 to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
        ),
        MethodId(
            classId = stringBuilderClassId,
            name = "append",
            returnType = stringBuilderClassId,
            parameters = listOf(charArrayClassId)
        ) to mapOf(
            THIS_PARAM_INDEX to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG)),
            1 to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG)),
            THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
            1 to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
        ),
        MethodId(
            classId = stringBuilderClassId,
            name = "toString",
            returnType = stringClassId,
            parameters = emptyList()
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringBufferClassId,
            name = "append",
            returnType = stringBufferClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(
            THIS_PARAM_INDEX to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG)),
            1 to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG))
        ),
        MethodId(
            classId = stringBufferClassId,
            name = "append",
            returnType = stringBufferClassId,
            parameters = listOf(charArrayClassId)
        ) to mapOf(
            THIS_PARAM_INDEX to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG)),
            1 to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG))
        ),
        MethodId(
            classId = stringBufferClassId,
            name = "toString",
            returnType = stringClassId,
            parameters = emptyList()
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringClassId,
            name = "concat",
            returnType = stringClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(
            THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
            1 to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))
        ),
        MethodId(
            classId = stringClassId,
            name = "toCharArray",
            returnType = charArrayClassId,
            parameters = emptyList()
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        // TODO should it always be pass through?
        MethodId(
            classId = stringClassId,
            name = "replace",
            returnType = stringClassId,
            parameters = listOf(charSequenceClassId, charSequenceClassId)
        ) to mapOf(
            THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
            2 to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG)),
        ),
        MethodId(
            classId = stringClassId,
            name = "toLowerCase",
            returnType = stringClassId,
            parameters = emptyList()
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringClassId,
            name = "toLowerCase",
            returnType = stringClassId,
            parameters = listOf(localeClassId)
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringClassId,
            name = "toUpperCase",
            returnType = stringClassId,
            parameters = emptyList()
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringClassId,
            name = "toUpperCase",
            returnType = stringClassId,
            parameters = listOf(localeClassId)
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringClassId,
            name = "substring",
            returnType = stringClassId,
            parameters = listOf(intClassId)
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringClassId,
            name = "substring",
            returnType = stringClassId,
            parameters = listOf(intClassId, intClassId)
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = stringClassId,
            name = "toString",
            returnType = stringClassId,
            parameters = emptyList()
        ) to mapOf(THIS_PARAM_INDEX to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),
        // TODO we should detect CharSequence argument here
        /*MethodId(
            classId = stringClassId,
            name = "valueOf",
            returnType = stringClassId,
            parameters = listOf(objectClassId)
        ) to mapOf(1 to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))),*/
        ConstructorId(
            classId = stringClassId,
            parameters = listOf(stringClassId)
        ) to mapOf(1 to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG))),
        ConstructorId(
            classId = stringClassId,
            parameters = listOf(charArrayClassId)
        ) to mapOf(1 to (THIS_PARAM_INDEX to setOf(SQL_INJECTION_FLAG))),
        MethodId(
            classId = systemCLassId,
            name = "arraycopy",
            returnType = voidClassId,
            parameters = listOf(objectClassId, intClassId, objectClassId, intClassId, intClassId)
        ) to mapOf(1 to (3 to setOf(SQL_INJECTION_FLAG))),
        // TODO we need to process only array of strings here, and mark as tainted if that array contains tainted element
        /*MethodId(
            classId = arraysCLassId,
            name = "toString",
            returnType = stringClassId,
            parameters = listOf(objectArrayClassId)
        ) to mapOf(?? to (RETURN_VALUE_INDEX to setOf(SQL_INJECTION_FLAG))), */
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

    sealed class TaintPattern(val pattern: Regex)

    class TaintSource(
        pattern: Regex,
        val taintOut: Set<Int>,
        val taintKinds: TaintKinds,
        val condition: Condition?
    ) : TaintPattern(pattern)

    class TaintSink(
        pattern: Regex,
        val taintSinks: Map<Int, Condition>
    ) : TaintPattern(pattern) {
        override fun toString(): String = "Pattern $pattern, sinks: $taintSinks"
    }

    // TODO what does it do exactly?
    class TaintSanitizer(
        pattern: Regex,
        val taintOut: Set<Int>,
        val taintKinds: TaintKinds,
    ) : TaintPattern(pattern)

    // TODO what does it do exactly?
    class TaintPassThrough(
        pattern: Regex,
        val taintIn: Set<Int>,
        val taintOut: Set<Int>,
        val taintKinds: TaintKinds,
        val condition: Condition?
    ) : TaintPattern(pattern) {
        override fun toString(): String =
            "Pattern: $pattern, taintIn: $taintIn, taintOut: $taintOut, taintKinds: $taintKinds"
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
class TaintAnalysisError(message: String, val taintSink: Stmt, val sinkSourcePosition: Int? = null) : Error(message)

typealias ParamIndexToTaintFlags = Map<Int, Set<String>>
private typealias SourceIndex = Int
private typealias TargetIndex = Int
typealias PassThroughArgument = Map<SourceIndex, Pair<TargetIndex, Set<String>>>
