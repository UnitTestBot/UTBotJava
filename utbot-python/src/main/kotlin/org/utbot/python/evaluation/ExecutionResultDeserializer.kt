package org.utbot.python.evaluation

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.python.code.DictMemoryObject
import org.utbot.python.code.ListMemoryObject
import org.utbot.python.code.MemoryDump
import org.utbot.python.code.MemoryObject
import org.utbot.python.code.ReduceMemoryObject
import org.utbot.python.code.ReprMemoryObject

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
        return jsonAdapter.fromJson(content)
    }

    fun parseMemoryDump(content: String): MemoryDump? {
        return jsonAdapterMemoryDump.fromJson(content)
    }
}

sealed class PythonExecutionResult

data class SuccessExecution(
    val isException: Boolean,
    val statements: List<Int>,
    val missedStatements: List<Int>,
    val stateBefore: String,
    val stateAfter: String,
    val argsIds: List<String>,
    val kwargsIds: List<String>,
    val resultId: String,
): PythonExecutionResult()

data class FailExecution(
    val exception: String,
): PythonExecutionResult()
