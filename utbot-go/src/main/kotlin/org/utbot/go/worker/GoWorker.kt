package org.utbot.go.worker

import com.beust.klaxon.Klaxon
import org.utbot.go.api.GoUtArrayModel
import org.utbot.go.api.GoUtComplexModel
import org.utbot.go.api.GoUtPrimitiveModel
import org.utbot.go.api.GoUtStructModel
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.util.convertObjectToJsonString
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

class GoWorker(
    socket: Socket,
) {
    private val reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

    private fun GoUtModel.convertToRawValue(): RawValue = when (val model = this) {
        is GoUtComplexModel -> PrimitiveValue(
            model.typeId.name,
            "${model.realValue.toValueGoCode()}@${model.imagValue.toValueGoCode()}"
        )

        is GoUtArrayModel -> ArrayValue(
            model.typeId.name,
            model.typeId.elementTypeId!!.canonicalName,
            model.length,
            model.getElements(model.typeId.elementTypeId!!).map { it.convertToRawValue() }
        )

        is GoUtStructModel -> StructValue(
            model.typeId.canonicalName,
            model.value.map {
                StructValue.FieldValue(
                    it.fieldId.name,
                    it.model.convertToRawValue(),
                    it.fieldId.isExported
                )
            }
        )

        is GoUtPrimitiveModel -> PrimitiveValue(model.typeId.name, model.value.toString())

        else -> error("Converting ${model.javaClass} to RawValue is not supported")
    }

    fun sendFuzzedParametersValues(parameters: List<GoUtModel>) {
        val rawValues = parameters.map { it.convertToRawValue() }
        val json = convertObjectToJsonString(rawValues)
        writer.write(json)
        writer.flush()
    }

    fun receiveRawExecutionResult(): RawExecutionResult {
        val length = reader.readLine().toInt()
        val buffer = CharArray(length)
        reader.read(buffer)
        return Klaxon().parse(String(buffer)) ?: error("")
    }
}