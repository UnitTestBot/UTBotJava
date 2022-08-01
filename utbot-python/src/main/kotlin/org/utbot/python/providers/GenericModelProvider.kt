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
        fun parseList(matchResult: MatchResult) = sequence {
            val genericType = matchResult.groupValues[0]
            val syntheticGenericType = FuzzedMethodDescription(
                "${description.name}<syntheticGenericList>",
                voidClassId,
                listOf(ClassId(genericType)),
                description.concreteValues
            )
            val models = fuzz(syntheticGenericType, concreteTypesModelProvider)
                .chunked(100) { list -> list.drop(Random.nextInt(list.size)) }  // 100 is random max size
                .map { list ->
                    PythonListModel(
                        PythonListModel.classId,
                        list.size,
                        list.flatten().map { it.model },
                    )
                }
            models.forEach {
                yield(FuzzedParameter(0, it.fuzzed()))
            }
        }

//        fun parseDict(matchResult: MatchResult) = sequence {
//            TODO("NotImplementedError")
//        }
//
//        fun parseSet(matchResult: MatchResult) = sequence {
//            TODO("NotImplementedError")
//        }

        val modelRegexMap = mapOf<Regex, (MatchResult) -> Unit>(
            Regex(".*[Ll]ist\\[(.*)]}") to { matchResult -> parseList(matchResult) },
            Regex("typing.List\\[(.*)]}") to { matchResult -> parseList(matchResult) },
//            Regex(".*[Dd]ict\\[(.*)]}") to { matchResult -> parseDict(matchResult) },
//            Regex("typing.Dict\\[(.*)]}") to { matchResult -> parseDict(matchResult) },
//            Regex(".*[Ss]et\\[(.*)]}") to { matchResult -> parseSet(matchResult) },
//            Regex("typing.Set\\[(.*)]}") to { matchResult -> parseSet(matchResult) },
        )

//        val generated = Array(description.parameters.size) { 0 }
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val annotation = classId.name
            modelRegexMap.entries.forEach { (regex, action) ->
                val result = regex.matchEntire(annotation)
                if (result != null) {
                    action(result)
                }
            }
        }
    }
}

