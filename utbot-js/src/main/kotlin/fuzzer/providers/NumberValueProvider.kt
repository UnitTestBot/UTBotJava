package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsBasic
import fuzzer.JsMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.PrimitivesModelProvider.fuzzed
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.DefaultFloatBound
import org.utbot.fuzzing.seeds.IEEE754Value

//object NumberValueProvider : ValueProvider<JsClassId, FuzzedValue, JsMethodDescription> {
//
//    override fun accept(type: JsClassId): Boolean {
//        return (type.isJsBasic || type == jsNumberClassId) && type != jsDoubleClassId
//    }
//
//    private val randomStubWithNoUsage = Random(0)
//    private val configurationStubWithNoUsage = Configuration()
//
//    private fun BitVectorValue.change(func: BitVectorValue.() -> Unit): BitVectorValue {
//        return Mutation<KnownValue> { _, _, _ ->
//            BitVectorValue(this).apply { func() }
//        }.mutate(this, randomStubWithNoUsage, configurationStubWithNoUsage) as BitVectorValue
//    }
//
//    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, FuzzedValue>> = sequence {
//        description.concreteValues.forEach { (_, v, c) ->
//            val value = BitVectorValue.fromValue(v)
//            val values = listOfNotNull(
//                value,
//                when (c) {
//                    EQ, NE, LE, GT -> value.change { inc() }
//                    LT, GE -> value.change { dec() }
//                    else -> null
//                }
//            )
//            values.forEach {
//                yield(Seed.Known(it) { known ->
//                    JsPrimitiveModel(known.toLong()).fuzzed {
//                        summary = "%var% = ${known.toLong()}"
//                    }
//                })
//            }
//        }
//        Signed.values().forEach { bound ->
//            val s = 64
//            val value = bound(s)
//            yield(Seed.Known(value) { known ->
//                JsPrimitiveModel(known.toLong()).fuzzed {
//                    summary = "%var% = ${known.toLong()}"
//                }
//            })
//        }
//    }
//}

object FloatValueProvider : ValueProvider<JsClassId, FuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic
    }
    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, FuzzedValue>> = sequence {
        description.concreteValues.forEach { (_, v, _) ->
            yield(Seed.Known(IEEE754Value.fromValue(v)) { known ->
                JsPrimitiveModel(known.toDouble()).fuzzed {
                    summary = "%var% = ${known.toDouble()}"
                }
            })
        }
        DefaultFloatBound.values().forEach { bound ->
            // All numbers in JavaScript are like Double in Java/Kotlin
            yield(Seed.Known(bound(52, 11)) { known ->
                JsPrimitiveModel(known.toDouble()).fuzzed {
                    summary = "%var% = ${known.toDouble()}"
                }
            })
        }
    }
}