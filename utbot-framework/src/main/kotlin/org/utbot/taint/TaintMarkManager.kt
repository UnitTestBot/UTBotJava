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
     * Sets taint [marks] to the taint vector at address [addr] if [condition] is true.
     */
    fun setMarks(
        memory: Memory,
        addr: UtAddrExpression,
        marks: TaintMarks,
        condition: UtBoolExpression
    ): SymbolicStateUpdate {
        if (marks.isEmpty()) {
            return SymbolicStateUpdate()
        }

        val newTaintVector = constructTaintVector(marks)
        val oldTaintVector = memory.taintVector(addr)

        val taintUpdateExpr = UtIteExpression(
            condition,
            thenExpr = newTaintVector,
            elseExpr = oldTaintVector
        )

        return updateAddr(addr, taintUpdateExpr)
    }

    /**
     * Removes taint [marks] from the taint vector at address [addr] in [memory] if [condition] is true.
     */
    fun clearMarks(
        memory: Memory,
        addr: UtAddrExpression,
        marks: TaintMarks,
        condition: UtBoolExpression
    ): SymbolicStateUpdate {
        if (marks.isEmpty()) {
            return SymbolicStateUpdate()
        }

        val taintMarks = constructTaintVector(marks).toLongValue()
        val taintMarksNegated = UtBvNotExpression(taintMarks).toLongValue()
        val oldTaintVectorExpr = memory.taintVector(addr)
        val newTaintVectorExpr = And(taintMarksNegated, oldTaintVectorExpr.toLongValue())

        val taintUpdateExpr = UtIteExpression(
            condition,
            thenExpr = newTaintVectorExpr,
            elseExpr = oldTaintVectorExpr
        )

        return updateAddr(addr, taintUpdateExpr)
    }

    /**
     * Passes taint [marks] contained in the taint vector at [fromAddr]
     * from [fromAddr] to [toAddr] in [memory] if [condition] is true.
     */
    fun passMarks(
        memory: Memory,
        fromAddr: UtAddrExpression,
        toAddr: UtAddrExpression,
        marks: TaintMarks,
        condition: UtBoolExpression
    ): SymbolicStateUpdate {
        if (marks.isEmpty()) {
            return SymbolicStateUpdate()
        }

        val taintMarks = constructTaintVector(marks).toLongValue()
        val oldTaintVectorFrom = memory.taintVector(fromAddr).toLongValue()
        val intersection = And(taintMarks, oldTaintVectorFrom).toLongValue()

        val oldTaintVectorExprTo = memory.taintVector(toAddr)
        val newTaintVectorExprTo = Or(intersection, oldTaintVectorExprTo.toLongValue())

        val taintUpdateExpr = UtIteExpression(
            condition,
            thenExpr = newTaintVectorExprTo,
            elseExpr = oldTaintVectorExprTo
        )

        return updateAddr(toAddr, taintUpdateExpr)
    }

    // internal

    /**
     * Returns taint vector that represents [marks].
     */
    private fun constructTaintVector(marks: TaintMarks): UtBvExpression {
        val taintedLongValue = when (marks) {
            TaintMarksAll -> -1L // 0b11..1111
            is TaintMarksSet -> marks.marks.fold(initial = 0L) { acc, mark ->
                acc or markRegistry.idByMark(mark)
            }
        }
        return mkLong(taintedLongValue)
    }

    private fun updateAddr(addr: UtAddrExpression, updateTaintVectorExpr: UtExpression): SymbolicStateUpdate =
        SymbolicStateUpdate(
            memoryUpdates = MemoryUpdate(
                taintArrayUpdate = persistentListOf(addr to updateTaintVectorExpr)
            )
        )
}