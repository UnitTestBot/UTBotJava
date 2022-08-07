package org.utbot.python.providers

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.*
import org.utbot.python.code.dict
import org.utbot.python.utils.*
import java.lang.Integer.min
import kotlin.random.Random

object GenericModelProvider: ModelProvider {
    private val concreteTypesModelProvider = ModelProvider.of(
        ConstantModelProvider,
        DefaultValuesModelProvider,
        GenericModelProvider
    )

    private const val maxGenNum = 10

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        fun <T: UtModel> fuzzGeneric(
            parameters: List<ClassId>,
            index: Int,
            modelConstructor: (List<List<FuzzedValue>>) -> T
        ) = sequence {
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
                    yield(FuzzedParameter(index, it.fuzzed()))
                }
        }

        fun genList(listAnnotation: ListAnnotation, index: Int): Sequence<FuzzedParameter> {
            val genericType = ClassId(listAnnotation.elemAnnotation)
            return fuzzGeneric(listOf(genericType), index) { list ->
                PythonListModel(
                    PythonListModel.classId,
                    list.size,
                    list.flatten().map { it.model }
                )
            }
        }

        fun genDict(dictAnnotation: DictAnnotation, index: Int): Sequence<FuzzedParameter> {
            val genericKeyType = ClassId(dictAnnotation.keyAnnotation)
            val genericValueType = ClassId(dictAnnotation.valueAnnotation)
            return fuzzGeneric(listOf(genericKeyType, genericValueType), index) { list ->
                    PythonDictModel(
                        PythonDictModel.classId,
                        list.size,
                        list.associate { pair ->
                            pair[0].model to pair[1].model
                        }
                    )
                }
        }

        fun genSet(setAnnotation: SetAnnotation, index: Int): Sequence<FuzzedParameter> {
            val genericType = ClassId(setAnnotation.elemAnnotation)
            return fuzzGeneric(listOf(genericType), index) { list ->
                    PythonSetModel(
                        PythonSetModel.classId,
                        list.size,
                        list.flatten().map { it.model }.toSet(),
                    )
                }
        }

        description.parametersMap.forEach { (classId, parameterIndices) ->
            val annotation = classId.name
            val parsedAnnotation = parseGeneric(annotation) ?: return@forEach
            parameterIndices.forEach { index ->
                val generatedModels = when (parsedAnnotation) {
                    is ListAnnotation -> genList(parsedAnnotation, index)
                    is DictAnnotation -> genDict(parsedAnnotation, index)
                    is SetAnnotation -> genSet(parsedAnnotation, index)
                }
                yieldAll(generatedModels.take(maxGenNum))
            }
        }
    }
}

fun Sequence<List<FuzzedValue>>.randomChunked(): Sequence<List<List<FuzzedValue>>> {
    val seq = this
    val maxSize = 15
    val itemsToGenerateFrom = seq.take(20).toList()
    return sequenceOf(emptyList<List<FuzzedValue>>()) + generateSequence {
        if (itemsToGenerateFrom.isEmpty())
            return@generateSequence null
        val size = Random.nextInt(1, min(maxSize, itemsToGenerateFrom.size) + 1)
        (0 until size).map {
            val index = Random.nextInt(0, itemsToGenerateFrom.size)
            itemsToGenerateFrom[index]
        }
    }
}
