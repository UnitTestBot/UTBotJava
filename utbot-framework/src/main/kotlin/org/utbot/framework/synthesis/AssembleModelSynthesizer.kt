package org.utbot.framework.synthesis

import org.utbot.framework.modifications.StatementsStorage
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.synthesis.postcondition.constructors.ModelBasedPostConditionConstructor
import org.utbot.framework.synthesis.postcondition.constructors.toSoot
import java.nio.file.Path
import java.util.PriorityQueue

class Synthesizer(
    statementsStorage: StatementsStorage,
    val model: UtCompositeModel,
    val depth: Int = 4
) {
    val initStmt = ObjectUnit(model.classId)
    val queue = SynthesisUnitQueue(depth)

    private val postConditionChecker = ModelBasedPostConditionConstructor(model)
    private val producer = LeafExpanderProducer(statementsStorage)
    private val unitChecker = SynthesisUnitChecker(model.classId.toSoot())

    fun synthesize(): UtModel? {
        queue.push(initStmt)

        while (!queue.isEmpty()) {

            val unit = queue.poll()!!
            System.err.println("Visiting state: $unit")

            val assembleModel = unitChecker.tryGenerate(unit, postConditionChecker)
            if (assembleModel != null) {
                System.err.println("Found!")
                return assembleModel
            }

            val newStates = producer.produce(unit)
            newStates.forEach { queue.push(it) }
        }

        return null
    }
}
class SynthesisUnitQueue(
    val depth: Int
) {
    private val queue = PriorityQueue<CallCount>()

    fun push(unit: SynthesisUnit) =
        CallCount(unit).run {
            if (methodsCount <= depth) {
                queue.offer(this)
            } else {
                false
            }
        }

    fun poll() =
        queue.poll()?.unit

    fun isEmpty() =
        queue.isEmpty()

    class CallCount(
        val unit: SynthesisUnit
    ) : Comparable<CallCount> {
        val methodsCount = unit.countMethodCalls()

        override fun compareTo(other: CallCount): Int =
            methodsCount.compareTo(other.methodsCount)

        private fun SynthesisUnit.countMethodCalls(): Int = when (this) {
            is ObjectUnit -> 0
            is MethodUnit -> 1 + this.params.fold(0) { sum, unit -> sum + unit.countMethodCalls() }
        }
    }
}

