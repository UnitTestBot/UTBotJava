package org.utbot.python.fuzzing.provider

import mu.KotlinLogging
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonNdarrayClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utpython.types.*
import kotlin.math.abs


private val logger = KotlinLogging.logger {}
private const val SHAPE_SIZE = 3
private const val MAX_SHAPE_DIGITS = 2

class NDArrayValueProvider(
    private val typeStorage: PythonTypeHintsStorage
) : PythonValueProvider {

    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonNdarrayClassId.canonicalName
    }

    @Suppress("UNCHECKED_CAST")
    override fun generate(
        description: PythonMethodDescription, type: FuzzedUtType
    ) = sequence {
        val param = type.utType.pythonAnnotationParameters()
        val intType = typeStorage.pythonInt
        val listType = typeStorage.pythonList

        yield(Seed.Recursive(
            construct = Routine.Create(
                listOf(
                    listType
                        .pythonDescription()
                        .createTypeWithNewAnnotationParameters(listType, listOf(intType)).toFuzzed()
                )
            ) {
                PythonFuzzedValue(
                    PythonTree.NDArrayNode(
                        emptyMap<Int, PythonTree.PythonTreeNode>().toMutableMap(),
                        ((it.first().tree as PythonTree.ListNode).items as Map<Int, PythonTree.PrimitiveNode>).values.map { node ->
                            abs(node.repr.take(MAX_SHAPE_DIGITS).toInt()) % 10
                        }.take(SHAPE_SIZE).let { self ->
                            if (self.fold(1, Int::times) == 0){
                                listOf(0)
                            } else {
                                self
                            }
                        } // TODO: Rethink logic
                    ), "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = sequence {
                yield(Routine.Call((0 until 1000000).map { param[1] }.toFuzzed()) { instance, arguments ->
                    val obj = instance.tree as PythonTree.NDArrayNode
                    (0 until obj.dimensions.fold(1, Int::times)).map {
                        obj.items[it] = arguments[it].tree
                    }
                })
            },
            empty = Routine.Empty { PythonFuzzedValue(PythonTree.FakeNode) }
        )

        )
    }
}