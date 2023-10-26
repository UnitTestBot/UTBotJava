package org.utbot.python.evaluation.serialization

import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object ExecutionResultDeserializer {
    private val moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(PythonExecutionResult::class.java, "status")
                .withSubtype(SuccessExecution::class.java, "success")
                .withSubtype(FailExecution::class.java, "fail")
        )
        .add(
            PolymorphicJsonAdapterFactory.of(MemoryObject::class.java, "strategy")
                .withSubtype(ReprMemoryObject::class.java, "repr")
                .withSubtype(ListMemoryObject::class.java, "list")
                .withSubtype(DictMemoryObject::class.java, "dict")
                .withSubtype(ReduceMemoryObject::class.java, "reduce")
        )
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(PythonExecutionResult::class.java)
    private val jsonAdapterMemoryDump = moshi.adapter(MemoryDump::class.java)

    fun parseExecutionResult(content: String): PythonExecutionResult? {
        try {
            return jsonAdapter.fromJson(content) ?: error("Parsing error with: $content")
        } catch (_: JsonEncodingException) {
            println(content)
        }
        return null
    }

    fun parseMemoryDump(content: String): MemoryDump? {
        return jsonAdapterMemoryDump.fromJson(content)
    }
}

sealed class PythonExecutionResult

data class SuccessExecution(
    val isException: Boolean,
    val statements: List<String>,
    val missedStatements: List<String>,
    val stateInit: String,
    val stateBefore: String,
    val stateAfter: String,
    val diffIds: List<String>,
    val argsIds: List<String>,
    val kwargsIds: Map<String, String>,
    val resultId: String,
): PythonExecutionResult()

data class FailExecution(
    val exception: String,
): PythonExecutionResult()
