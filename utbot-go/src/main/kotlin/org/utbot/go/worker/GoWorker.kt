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
    socket: Socket,
    val function: GoUtFunction
) {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

    fun sendFuzzedParametersValues(parameters: List<GoUtModel>, aliases: Map<GoPackage, String?>) {
        val rawValues = parameters.map { it.convertToRawValue(function.sourcePackage, aliases) }
        val json = convertObjectToJsonString(rawValues)
        writer.write(json)
        writer.flush()
    }

    fun receiveRawExecutionResult(): RawExecutionResult {
        val length = reader.readLine().toInt()
        val buffer = CharArray(length)
        reader.read(buffer)
        return Klaxon().parse(String(buffer)) ?: error("Error with parsing json as raw execution result")
    }
}