package fuzzer.providers

import fuzzer.providers.JsPrimitivesModelProvider.matchClassId
import fuzzer.providers.JsPrimitivesModelProvider.primitivesForString
import fuzzer.providers.JsStringModelProvider.mutate
import fuzzer.providers.JsStringModelProvider.random
import framework.api.js.JsClassId
import framework.api.js.JsMultipleClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsPrimitive
import framework.api.js.util.jsStringClassId
import framework.api.js.util.toJsClassId
import org.utbot.fuzzer.*

object JsMultipleTypesModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val parametersFiltered = description.parametersMap.filter { (classId, _) ->
            classId is JsMultipleClassId
        }
        parametersFiltered.forEach { (jsMultipleClassId, indices) ->
            val types = (jsMultipleClassId as JsMultipleClassId).types
            types.forEach { classId ->
                when {
                    classId.isJsPrimitive -> {
                        val concreteValuesFiltered = description.concreteValues.filter { (localClassId, _) ->
                            (localClassId as JsClassId).isJsPrimitive
                        }
                        concreteValuesFiltered.forEach { (_, value, op) ->
                            sequenceOf(
                                JsPrimitiveModel(value).fuzzed { summary = "%var% = $value" },
                                JsConstantsModelProvider.modifyValue(value, op as FuzzedContext.Comparison)
                            ).filterNotNull()
                                .forEach { m ->
                                    indices.forEach { index ->
                                        yield(FuzzedParameter(index, m))
                                    }
                                }
                        }
                        matchClassId(classId).forEach { value ->
                            indices.forEach { index -> yield(FuzzedParameter(index, value)) }
                        }
                    }

                    classId == jsStringClassId -> {
                        val concreteValuesFiltered = description.concreteValues
                            .asSequence()
                            .filter { (classId, _) -> classId.toJsClassId() == jsStringClassId }
                        concreteValuesFiltered.forEach { (_, value, op) ->
                            listOf(value, mutate(random, value as? String, op as FuzzedOp))
                                .asSequence()
                                .filterNotNull()
                                .map { JsPrimitiveModel(it) }
                                .forEach { m ->
                                    indices.forEach { index ->
                                        yield(
                                            FuzzedParameter(
                                                index,
                                                m.fuzzed { summary = "%var% = string" }
                                            )
                                        )
                                    }
                                }
                        }
                        primitivesForString().forEach { value ->
                            indices.forEach { index -> yield(FuzzedParameter(index, value)) }
                        }
                    }

                    else -> throw IllegalStateException("Not yet implemented!")
                }
            }
        }
    }
}