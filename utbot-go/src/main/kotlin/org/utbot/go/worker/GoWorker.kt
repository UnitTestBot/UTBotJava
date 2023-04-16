package org.utbot.go.worker

import com.beust.klaxon.Klaxon
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.util.convertToRawValue
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.util.convertObjectToJsonString
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class GoWorker(
    private val socket: Socket,
    private val goPackage: GoPackage,
    private val readTimeoutMillis: Long
) {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

    data class TestInput(
        val functionName: String,
        val arguments: List<RawValue>
    )

    fun sendFuzzedParametersValues(
        function: GoUtFunction,
        arguments: List<GoUtModel>,
        aliases: Map<GoPackage, String?>
    ) {
        val rawValues = arguments.map { it.convertToRawValue(goPackage, aliases) }
        val testCase = TestInput(function.modifiedName, rawValues)
        val json = convertObjectToJsonString(testCase)
        writer.write(json)
        writer.flush()
    }

    fun receiveRawExecutionResult(): RawExecutionResult {
        socket.soTimeout = readTimeoutMillis.toInt()
        val length = reader.readLine().toInt()
        val buffer = CharArray(length)
        reader.read(buffer)
        return Klaxon().parse(String(buffer)) ?: error("Error with parsing json as raw execution result")
    }
}