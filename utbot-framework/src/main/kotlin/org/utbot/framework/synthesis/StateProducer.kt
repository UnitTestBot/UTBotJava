package org.utbot.framework.synthesis

import org.utbot.framework.modifications.StatementsStorage
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId

interface StateProducer<T> {
    fun produce(state: T): List<T>
}


class LeafExpanderProducer(
    statementsStorage: StatementsStorage
) : StateProducer<SynthesisUnit> {
    private val leafExpander = CompositeUnitExpander(statementsStorage)

    override fun produce(state: SynthesisUnit): List<SynthesisUnit> =
        when (state) {
            is MethodUnit -> {
                state.params.run {
                    flatMapIndexed { idx, leaf ->
                        val newLeafs = produce(leaf)
                        newLeafs.map { newLeaf ->
                            val newParams = toMutableList()
                            newParams[idx] = newLeaf
                            state.copy(params = newParams)
                        }
                    }
                }
            }
            is ObjectUnit -> leafExpander.expand(state)
            is NullUnit -> emptyList()
            is ReferenceToUnit -> emptyList()
            is ArrayUnit -> expandArray(state)
        }

    private fun expandArray(arrayUnit: ArrayUnit): List<SynthesisUnit> {
        if (arrayUnit.isPrimitive()) return emptyList()

        var current = arrayUnit

        while (true) {
            val index = current.currentIndex
            val elements = current.elements
            if (index >= elements.size) break

            val currentUnit = elements[index]
            val newLeafs = produce(currentUnit.second)
            if (newLeafs.isEmpty()) {
                val newElements = elements.toMutableList()
                for (i in 0..index) newElements[i] = current.bases[i]
                current = current.copy(
                    elements = newElements,
                    currentIndex = index + 1,
                )
            } else {
                return newLeafs.map {
                    val newElements = elements.toMutableList()
                    newElements[index] = currentUnit.first to it
                    current.copy(
                        elements = newElements,
                        currentIndex = index
                    )
                }
            }
        }

        return emptyList()
    }
}

class CompositeUnitExpander(
    private val statementsStorage: StatementsStorage
) {
    fun expand(objectUnit: ObjectUnit): List<MethodUnit> {
        if (objectUnit.isPrimitive()) {
            return emptyList()
        }
        if (objectUnit.classId !in statementsStorage.items.keys.map { it.classId }.toSet()) {
            statementsStorage.update(setOf(objectUnit.classId).expandable())
        }
        val mutators = findMutators(objectUnit.classId)

        val expanded = mutators.map { method ->
            MethodUnit(
                objectUnit.classId,
                method,
                (method.thisParamOrEmptyList() + method.parameters).map { ObjectUnit(it) }
            )
        }
        return expanded
    }


    private fun findMutators(classId: ClassId): List<ExecutableId> =
        statementsStorage.items
            .filter { (method, info) ->
                val sameClass = method.classId == classId
                val modifiesSomething = info.modifiedFields.any { it.declaringClass == classId }
                sameClass && modifiesSomething
            }
            .keys
            .filterIsInstance<ExecutableId>()
            .toList()
}

private fun ExecutableId.thisParamOrEmptyList() =
    if (this is MethodId && !this.isStatic) {
        listOf(this.classId)
    } else {
        emptyList()
    }
