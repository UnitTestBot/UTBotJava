package fuzzer.new

import framework.api.js.JsClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsNumberClassId
import org.utbot.fuzzer.FuzzedContext.Comparison.EQ
import org.utbot.fuzzer.FuzzedContext.Comparison.GE
import org.utbot.fuzzer.FuzzedContext.Comparison.GT
import org.utbot.fuzzer.FuzzedContext.Comparison.LE
import org.utbot.fuzzer.FuzzedContext.Comparison.LT
import org.utbot.fuzzer.FuzzedContext.Comparison.NE
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.PrimitivesModelProvider.fuzzed
import org.utbot.fuzzing.Configuration
import org.utbot.fuzzing.Mutation
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.Signed
import kotlin.random.Random

object NumberValueProvider : ValueProvider<JsClassId, FuzzedValue, JsMethodDescription> {

    override fun accept(type: JsClassId): Boolean {
        return type.isJsBasic || type == jsNumberClassId
    }

    private val randomStubWithNoUsage = Random(0)
    private val configurationStubWithNoUsage = Configuration()

    private fun BitVectorValue.change(func: BitVectorValue.() -> Unit): BitVectorValue {
        return Mutation<KnownValue> { _, _, _ ->
            BitVectorValue(this).apply { func() }
        }.mutate(this, randomStubWithNoUsage, configurationStubWithNoUsage) as BitVectorValue
    }

    override fun generate(description: JsMethodDescription, type: JsClassId): Sequence<Seed<JsClassId, FuzzedValue>> = sequence {
        description.concreteValues.forEach { (_, v, c) ->
            val value = BitVectorValue.fromValue(v)
            val values = listOfNotNull(
                value,
                when (c) {
                    EQ, NE, LE, GT -> value.change { inc() }
                    LT, GE -> value.change { dec() }
                    else -> null
                }
            )
            values.forEach {
                yield(Seed.Known(it) { known ->
                    JsPrimitiveModel(known.toLong()).fuzzed {
                        summary = "%var% = ${known.toLong()}"
                    }
                })
            }
        }
        Signed.values().forEach { bound ->
            val s = 64
            val value = bound(s)
            yield(Seed.Known(value) { known ->
                JsPrimitiveModel(known.toLong()).fuzzed {
                    summary = "%var% = ${known.toLong()}"
                }
            })
        }
    }
}