package org.utbot.taint

import kotlinx.collections.immutable.persistentListOf
import org.utbot.engine.*
import org.utbot.engine.pc.*
import org.utbot.engine.symbolic.SymbolicStateUpdate
import org.utbot.taint.TaintUtil.isEmpty
import org.utbot.taint.model.TaintMarksAll
import org.utbot.taint.model.TaintMark
import org.utbot.taint.model.TaintMarks
import org.utbot.taint.model.TaintMarksSet

typealias TaintVector = PrimitiveValue

/**
 * Manages taint vectors.
 */
class TaintMarkManager(private val markRegistry: TaintMarkRegistry) {

    /**
     * Returns true if taint vector at address [addr] in [memory] contains taint [mark].
     */
    fun containsMark(memory: Memory, addr: UtAddrExpression, mark: TaintMark): UtBoolExpression {
        val taintVector = memory.taintVector(addr).toLongValue()
        val taintMarkId = markRegistry.idByMark(mark)
        val taintMarkVector = mkLong(taintMarkId).toLongValue()
        return mkNot(mkEq(And(taintVector, taintMarkVector), mkLong(0L)))
    }

    /**
     * Returns true if taint vector at address [addr] in [memory] contains any taint mark.
     */
    fun containsAnyMark(memory: Memory, addr: UtAddrExpression): UtBoolExpression {
        val taintVector = memory.taintVector(addr).toLongValue()
        return mkNot(mkEq(taintVector, mkLong(0L).toLongValue()))
    }

    /**
     * Adds taint [marks] to the taint vector at address [addr].
     */
    fun addMarks(addr: UtAddrExpression, marks: TaintMarks): SymbolicStateUpdate =
        if (marks.isEmpty()) {
            SymbolicStateUpdate()
        } else {
            updateAddr(addr, constructTaintVector(marks))
        }

    /**
     * Removes taint [marks] from the taint vector at address [addr] in [memory].
     */
    fun clearMarks(memory: Memory, addr: UtAddrExpression, marks: TaintMarks): SymbolicStateUpdate {
        if (marks.isEmpty()) {
            return SymbolicStateUpdate()
        }

        val taintVector = constructTaintVector(marks)
        val negatedTaintVector = UtBvNotExpression(taintVector).toLongValue()
        val actualTaintVector = memory.taintVector(addr).toLongValue()
        val updateTaintVector = And(negatedTaintVector, actualTaintVector).toLongValue()

        return updateAddr(addr, updateTaintVector)
    }

    /**
     * Passes taint [marks] contained in the taint vector at [fromAddr] from [fromAddr] to [toAddr] in [memory].
     */
    fun passMarks(memory: Memory, fromAddr: UtAddrExpression, toAddr: UtAddrExpression, marks: TaintMarks): SymbolicStateUpdate {
        if (marks.isEmpty()) {
            return SymbolicStateUpdate()
        }

        val taintVector = constructTaintVector(marks)
        val sourceTaintVector = memory.taintVector(fromAddr).toLongValue()
        val intersection = And(taintVector, sourceTaintVector).toLongValue()
        val actualVector = memory.taintVector(toAddr).toLongValue()

        return updateAddr(toAddr, Or(actualVector, intersection).toLongValue())
    }

    // internal

    /**
     * Returns taint vector that represents [marks].
     */
    private fun constructTaintVector(marks: TaintMarks): TaintVector {
        val taintedLongValue = when (marks) {
            TaintMarksAll -> -1L // 0b11..1111
            is TaintMarksSet -> marks.marks.fold(initial = 0L) { acc, mark ->
                acc or markRegistry.idByMark(mark)
            }
        }
        return UtBvLiteral(taintedLongValue, UtLongSort).toLongValue()
    }

    private fun updateAddr(addr: UtAddrExpression, updateTaintVector: TaintVector): SymbolicStateUpdate =
        SymbolicStateUpdate(memoryUpdates = MemoryUpdate(
            taintArrayUpdate = persistentListOf(addr to updateTaintVector.expr))
        )
}