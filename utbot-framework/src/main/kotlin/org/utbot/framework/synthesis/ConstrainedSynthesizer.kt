package org.utbot.framework.synthesis

import mu.KotlinLogging
import org.utbot.engine.ResolvedModels
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.framework.modifications.StatementsStorage
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.synthesis.postcondition.constructors.ConstraintBasedPostConditionConstructor
import org.utbot.framework.synthesis.postcondition.constructors.toSoot

internal fun Collection<ClassId>.expandable() = filter { !it.isArray && !it.isPrimitive }.toSet()

class ConstrainedSynthesizer(
    val parameters: ResolvedModels,
    val depth: Int = 4
) {
    private val logger = KotlinLogging.logger("ConstrainedSynthesizer")
    private val statementStorage = StatementsStorage().also { storage ->
        storage.update(parameters.parameters.map { it.classId }.expandable())
    }

    private val queue = MultipleSynthesisUnitQueue(
        parameters,
        LeafExpanderProducer(statementStorage),
        depth
    )
    private val unitChecker = ConstrainedSynthesisUnitChecker(objectClassId.toSoot())

    fun synthesize(): List<UtModel>? {
        while (!queue.isEmpty()) {
            val units = queue.poll()
            logger.debug { "Visiting state: $units" }

            val assembleModel = unitChecker.tryGenerate(units, parameters)
            if (assembleModel != null) {
                logger.debug { "Found $assembleModel" }
                return assembleModel
            }
        }
        return null
    }
}
fun List<UtModel>.toSynthesisUnits() = map {
    when (it) {
        is UtNullModel -> NullUnit(it.classId)
        is UtPrimitiveConstraintModel -> ObjectUnit(it.classId)
        is UtReferenceConstraintModel -> ObjectUnit(it.classId)
        is UtReferenceToConstraintModel -> RefUnit(it.classId, indexOf(it.reference))
        else -> error("Only UtSynthesisModel supported")
    }
}

class MultipleSynthesisUnitQueue(
    val parameters: ResolvedModels,
    val producer: LeafExpanderProducer,
    val depth: Int
) {
    private val inits = parameters.parameters.toSynthesisUnits()
    private val queues = inits.map { init -> initializeQueue(init) }.toMutableList()
    private var hasNext = true

    fun isEmpty(): Boolean = !hasNext

    fun poll(): List<SynthesisUnit> {
        val results = queues.map { it.peek()!! }
        increase()
        return results
    }

    private fun increase() {
        var shouldGoNext = true
        var index = 0
        while (shouldGoNext) {
            pollUntilFullyDefined(queues[index])
            if (queues[index].isEmpty()) {
                queues[index] = initializeQueue(inits[index])
                index++
                if (index >= queues.size) {
                    hasNext = false
                    return
                }
                shouldGoNext = true
            } else {
                shouldGoNext = false
            }
        }
    }

    private fun initializeQueue(unit: SynthesisUnit): SynthesisUnitQueue = SynthesisUnitQueue(depth).also { queue ->
        when {
            unit is ObjectUnit && unit.isPrimitive() -> queue.push(unit)
            unit is NullUnit -> queue.push(unit)
            unit is RefUnit -> queue.push(unit)
            else -> producer.produce(unit).forEach { queue.push(it) }
        }
        peekUntilFullyDefined(queue)
    }

    private fun peekUntilFullyDefined(queue: SynthesisUnitQueue) {
        if (queue.isEmpty()) return
        while (!queue.peek()!!.isFullyDefined()) {
            val state = queue.poll()!!
            producer.produce(state).forEach { queue.push(it) }
            if (queue.isEmpty()) return
        }
    }

    private fun pollUntilFullyDefined(queue: SynthesisUnitQueue) {
        val state = queue.poll()!!
        producer.produce(state).forEach { queue.push(it) }
        peekUntilFullyDefined(queue)
    }
}