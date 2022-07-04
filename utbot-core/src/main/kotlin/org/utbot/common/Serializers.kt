package org.utbot.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

object IntRangeSerializer: KSerializer<IntRange> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("asd") {
            element<Int>("first")
            element<Int>("last")
        }

    override fun deserialize(decoder: Decoder): IntRange {
        return decoder.decodeStructure(descriptor) {
            var first = -1
            var last = -1

            while (true) {
                when(val index = decodeElementIndex(descriptor)) {
                    0 -> first = decodeIntElement(descriptor, 0)
                    1 -> last = decodeIntElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            IntRange(first, last)
        }
    }

    override fun serialize(encoder: Encoder, value: IntRange) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.first)
            encodeIntElement(descriptor, 1, value.last)
        }
    }
}
