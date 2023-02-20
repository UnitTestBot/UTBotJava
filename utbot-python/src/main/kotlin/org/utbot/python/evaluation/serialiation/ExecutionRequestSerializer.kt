package org.utbot.python.evaluation.serialiation

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object ExecutionRequestSerializer {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(ExecutionRequest::class.java)

    fun serializeRequest(request: ExecutionRequest): String? {
        return jsonAdapter.toJson(request)
    }
}

data class ExecutionRequest(
    val functionName: String,
    val imports: List<String>,
    val syspaths: List<String>,
    val argumentsIds: List<String>,
    val serializedMemory: String,
    val coverageDB: String,
    val filepath: String,
)
