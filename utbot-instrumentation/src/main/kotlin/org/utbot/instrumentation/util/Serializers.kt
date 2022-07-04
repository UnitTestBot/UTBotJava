package org.utbot.instrumentation.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.serializer
import org.utbot.framework.plugin.api.UtConcreteExecutionData
import org.utbot.instrumentation.util.Command.*

val executionDataSerializer = serializer<UtConcreteExecutionData>()

object InvokeMethodCommandSerializer : KSerializer<InvokeMethodCommand> {
    @InternalSerializationApi
    @ExperimentalSerializationApi
    override fun deserialize(decoder: Decoder): InvokeMethodCommand {
        return decoder.decodeStructure(descriptor) {
            var className: String? = null
            var signature: String? = null
            var executionData: UtConcreteExecutionData? = null

            while(true) {
                val index = decodeElementIndex(descriptor)

                when (index) {
                    0 -> className = decodeStringElement(descriptor, 0)
                    1 -> signature = decodeStringElement(descriptor, 1)
                    2 -> executionData = decodeSerializableElement(descriptor, 2, executionDataSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected token $index")
                }
            }

            return@decodeStructure InvokeMethodCommand(className!!, signature!!, emptyList(), executionData!!)
        }
    }

    @InternalSerializationApi
    @ExperimentalSerializationApi
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("InvokeMethodCommand") {
        element<String>("className")
        element<String>("signature")
        element<UtConcreteExecutionData>("parameters")
    }

    @InternalSerializationApi
    @ExperimentalSerializationApi
    override fun serialize(encoder: Encoder, value: InvokeMethodCommand) {
        val invokeCommand = value

        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, invokeCommand.className)
            encodeStringElement(descriptor, 1, invokeCommand.signature)
            encodeSerializableElement(descriptor, 2, executionDataSerializer, invokeCommand.parameters as UtConcreteExecutionData)
        }
    }
}