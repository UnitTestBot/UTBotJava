package org.utbot.python.providers

import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.fuzz
import org.utbot.python.framework.api.python.*
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.typing.DictAnnotation
import org.utbot.python.typing.ListAnnotation
import org.utbot.python.typing.SetAnnotation
import org.utbot.python.typing.parseGeneric
import java.lang.Integer.min
import kotlin.random.Random

const val MAX_CONTAINER_SIZE = 7

class GenericModelProvider(recursionDepth: Int) : PythonModelProvider(recursionDepth) {
    private val maxGenNum = 10

    override fun generate(description: PythonFuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        fun <T : UtModel> fuzzGeneric(
            parameters: List<NormalizedPythonAnnotation>,
            index: Int,
            modelConstructor: (List<List<FuzzedValue>>) -> T?
        ) = sequence inner@{
            if (recursionDepth <= 0)
                return@inner

            val syntheticGenericType = FuzzedMethodDescription(
                "${description.name}<syntheticGenericList>",
                pythonNoneClassId,
                parameters,
                description.concreteValues
            )

            fuzz(syntheticGenericType, getDefaultPythonModelProvider(recursionDepth - 1))
                .randomChunked()
                .mapNotNull(modelConstructor)
                .forEach {
                    yield(FuzzedParameter(index, it.fuzzed()))
                }
        }

        fun genList(listAnnotation: ListAnnotation, index: Int): Sequence<FuzzedParameter> {
            return fuzzGeneric(listOf(listAnnotation.elemAnnotation), index) { list ->
                PythonListModel(
                    list.size,
                    list.flatten().mapNotNull { it.model as? PythonModel }
                )
            }
        }

        fun genDict(dictAnnotation: DictAnnotation, index: Int): Sequence<FuzzedParameter> {
            return fuzzGeneric(listOf(dictAnnotation.keyAnnotation, dictAnnotation.valueAnnotation), index) { list ->
                if (list.any { it.any { value -> value.model !is PythonModel } })
                    return@fuzzGeneric null
                PythonDictModel(
                    list.size,
                    list.associate { pair ->
                        (pair[0].model as PythonModel) to (pair[1].model as PythonModel)
                    }
                )
            }
        }

        fun genSet(setAnnotation: SetAnnotation, index: Int): Sequence<FuzzedParameter> {
            return fuzzGeneric(listOf(setAnnotation.elemAnnotation), index) { list ->
                PythonSetModel(
                    list.size,
                    list.flatten().mapNotNull { it.model as? PythonModel }.toSet(),
                )
            }
        }

        description.parametersMap.forEach { (classId, parameterIndices) ->
            val parsedAnnotation = parseGeneric(classId as NormalizedPythonAnnotation) ?: return@forEach
            parameterIndices.forEach { index ->
                val generatedModels = when (parsedAnnotation) {
                    is ListAnnotation -> genList(parsedAnnotation, index)
                    is DictAnnotation -> genDict(parsedAnnotation, index)
                    is SetAnnotation -> genSet(parsedAnnotation, index)
                }
                yieldAll(
                    generatedModels.take(maxGenNum).distinctBy { fuzzedParameter ->
                        fuzzedParameter.value.model.toString()
                    }
                )
            }
        }
    }
}

fun Sequence<List<FuzzedValue>>.randomChunked(): Sequence<List<List<FuzzedValue>>> {
    val seq = this
    val itemsToGenerateFrom = seq.take(MAX_CONTAINER_SIZE * 2).toList()
    return sequenceOf(emptyList<List<FuzzedValue>>()) + generateSequence {
        if (itemsToGenerateFrom.isEmpty())
            return@generateSequence null
        val size = Random.nextInt(1, min(MAX_CONTAINER_SIZE, itemsToGenerateFrom.size) + 1)
        (0 until size).map {
            val index = Random.nextInt(0, itemsToGenerateFrom.size)
            itemsToGenerateFrom[index]
        }
    }
}
