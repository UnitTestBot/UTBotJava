package org.utbot.framework.synthesis

import mu.KotlinLogging
import org.utbot.engine.ResolvedConstraints
import org.utbot.framework.modifications.StatementsStorage
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.synthesis.postcondition.constructors.ConstraintBasedPostConditionConstructor
import org.utbot.framework.synthesis.postcondition.constructors.toSoot

class ConstrainedSynthesizer(
    val parameters: ResolvedConstraints,
    val depth: Int = 4
) {
    private val logger = KotlinLogging.logger("ConstrainedSynthesizer")
    private val statementStorage = StatementsStorage().also { storage ->
        storage.update(parameters.parameters.map { it.classId }.toSet())
    }

    private val postConditionChecker = ConstraintBasedPostConditionConstructor(parameters.parameters)
    private val queue = MultipleSynthesisUnitQueue(
        parameters,
        LeafExpanderProducer(statementStorage),
        depth
    )
    private val unitChecker = ConstrainedSynthesisUnitChecker(parameters.parameters.first().classId.toSoot())

    fun synthesize(): UtModel? {
        while (!queue.isEmpty()) {
            val units = queue.poll()

            val assembleModel = unitChecker.tryGenerate(units, postConditionChecker)
            if (assembleModel != null) {
                logger.debug { "Found $assembleModel" }
                return assembleModel
            }
        }
        return null
    }
}


class MultipleSynthesisUnitQueue(
    val parameters: ResolvedConstraints,
    val producer: LeafExpanderProducer,
    val depth: Int
) {
    private val inits = parameters.parameters.map { ObjectUnit(it.classId) }
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

    private fun initializeQueue(unit: ObjectUnit): SynthesisUnitQueue = SynthesisUnitQueue(depth).also { queue ->
        if (unit.isPrimitive()) queue.push(unit)
        else producer.produce(unit).forEach { queue.push(it) }
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