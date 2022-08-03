package org.utbot.python.providers

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.*
import kotlin.random.Random

object GenericModelProvider: ModelProvider {
    val concreteTypesModelProvider = ModelProvider.of(
        ConstantModelProvider,
        DefaultValuesModelProvider,
        GenericModelProvider
    )

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        fun <T: UtModel> fuzzGeneric(parameters: List<ClassId>, modelConstructor: (List<List<FuzzedValue>>) -> T) = sequence {
            val syntheticGenericType = FuzzedMethodDescription(
                "${description.name}<syntheticGenericList>",
                voidClassId,
                parameters,
                description.concreteValues
            )
            fuzz(syntheticGenericType, concreteTypesModelProvider)
                .randomChunked()
                .map(modelConstructor)
                .forEach {
                    yield(FuzzedParameter(0, it.fuzzed()))
                }
        }

        fun parseList(matchResult: MatchResult): Sequence<FuzzedParameter> {
            val genericType = if (matchResult.groupValues.size >= 2) ClassId(matchResult.groupValues[1]) else pythonAnyClassId
            return fuzzGeneric(listOf(genericType)) { list ->
                PythonListModel(
                    PythonListModel.classId,
                    list.size,
                    list.flatten().map { it.model }
                )
            }
        }

        fun parseDict(matchResult: MatchResult): Sequence<FuzzedParameter> {
            val genericKeyType = if (matchResult.groupValues.size >= 2) ClassId(matchResult.groupValues[1]) else pythonAnyClassId
            val genericValueType = if (matchResult.groupValues.size >= 3) ClassId(matchResult.groupValues[2]) else pythonAnyClassId
            return fuzzGeneric(listOf(genericKeyType, genericValueType)) { list ->
                    PythonDictModel(
                        PythonDictModel.classId,
                        list.size,
                        list.associate { pair ->
                            pair[0].model to pair[1].model
                        }
                    )
                }
        }

        fun parseSet(matchResult: MatchResult): Sequence<FuzzedParameter> {
            val genericType = if (matchResult.groupValues.size >= 2) ClassId(matchResult.groupValues[1]) else pythonAnyClassId
            return fuzzGeneric(listOf(genericType)) { list ->
                    PythonSetModel(
                        PythonSetModel.classId,
                        list.size,
                        list.flatten().map { it.model }.toSet(),
                    )
                }
        }

        val modelRegexMap = mapOf<Regex, (MatchResult) -> Sequence<FuzzedParameter>>(
            Regex("builtins.list\\[(.*)]") to { matchResult -> parseList(matchResult) },
            Regex("[Ll]ist\\[(.*)]") to { matchResult -> parseList(matchResult) },
            Regex("typing.List\\[(.*)]") to { matchResult -> parseList(matchResult) },

            Regex("builtins.dict\\[(.*), *(.*)]") to { matchResult -> parseDict(matchResult) },
            Regex("[Dd]ict\\[(.*), *(.*)]") to { matchResult -> parseDict(matchResult) },
            Regex("typing.Dict\\[(.*), *(.*)]") to { matchResult -> parseDict(matchResult) },

            Regex("builtins.set\\[(.*)]") to { matchResult -> parseSet(matchResult) },
            Regex("[Ss]et\\[(.*)]") to { matchResult -> parseSet(matchResult) },
            Regex("typing.Set\\[(.*)]") to { matchResult -> parseSet(matchResult) },
        )

        description.parametersMap.forEach { (classId, parameterIndices) ->
            val annotation = classId.name
            parameterIndices.forEach { _ ->
                modelRegexMap.entries.forEach { (regex, action) ->
                    val result = regex.matchEntire(annotation)
                    if (result != null) {
                        yieldAll(action(result).take(10))
                    }
                }
            }
        }
    }
}

fun Sequence<List<FuzzedValue>>.randomChunked(): Sequence<List<List<FuzzedValue>>> {
    val seq = this
    val maxSize = 15
    val listOfLists = (0 until maxSize).map { seq.take(10).toList() }
    return CartesianProduct(listOfLists, Random(0)).asSequence().map {
        it.take(Random.nextInt(maxSize))
    }
}
