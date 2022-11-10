package org.utbot.engine.taint

import org.utbot.engine.PrimitiveValue
import org.utbot.engine.SymbolicValue
import org.utbot.engine.TraversalContext
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.UtTrue
import org.utbot.engine.pc.mkAnd
import org.utbot.engine.pc.mkEq
import org.utbot.engine.pc.mkLong
import org.utbot.engine.pc.mkNot
import org.utbot.engine.pc.mkOr
import org.utbot.engine.pc.mkTrue
import org.utbot.engine.toLongValue
import soot.jimple.InvokeExpr

interface Condition {
    fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression

    fun collectRelatedTaintKinds(taintKinds: TaintKinds)
}

sealed class UnaryCondition(val condition: Condition) : Condition {
    override fun collectRelatedTaintKinds(
        taintKinds: TaintKinds
    ) = condition.collectRelatedTaintKinds(taintKinds)
}

sealed class MultiCondition(val conditionsList: Collection<Condition>) : Condition {
    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        conditionsList.forEach { it.collectRelatedTaintKinds(taintKinds) }
    }
}

@Suppress("UNUSED_PARAMETER")
sealed class ConstantCondition(val argument: Int, val value: String) : Condition {
    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        TODO("Not yet implemented")
    }
}

//sealed class LiteralArrayConstantCondition : ConstantCondition

sealed class AbstractTypeCondition(
    val argument: Int,
    val packageName: NameInformation,
    val className: NameInformation
) : Condition

class And(conditionsList: Collection<Condition>) : MultiCondition(conditionsList) {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression =
        mkAnd(conditionsList.map { it.toBoolExpr(traversalContext, taintValue, base, args, returnValue) })

    override fun toString(): String = "(and (${conditionsList.joinToString(", ")}))"
}

class Or(conditionsList: Collection<Condition>) : MultiCondition(conditionsList) {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression =
        mkOr(conditionsList.map { it.toBoolExpr(traversalContext, taintValue, base, args, returnValue) })
    override fun toString(): String = "(or (${conditionsList.joinToString(", ")}))"

}

class Not(condition: Condition) : UnaryCondition(condition) {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        val nestedExpression = condition.toBoolExpr(traversalContext, taintValue, base, args, returnValue)

        // To avoid situation when we have unsupported condition returning `true`, and
        // a condition that looks like `not true` instead of `not matches`.
        return if (nestedExpression is UtTrue) UtTrue else mkNot(nestedExpression)
    }

    override fun toString(): String = "(not $condition)"
}

class SinkLabel(val name: String) : Condition {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression = UtTrue // TODO

    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        // do nothing
    }
}

class TaintKind(val name: String) : Condition {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        val taintFlagId = taintAnalysis.idByFlag(name)
        val taintBv = mkLong(taintFlagId).toLongValue()

        return mkNot(mkEq(org.utbot.engine.And(taintBv, taintValue), mkLong(value = 0L)))
    }

    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        TODO("Not yet implemented")
    }

    override fun toString(): String = name
}

/**
 * Marks that a particular [argument] for a function is a const value (literal).
 */
class IsConstant(val argument: Int) : Condition {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        return UtTrue // TODO not implemente yet
    }

    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        TODO("Not yet implemented")
    }
}

class ConstantEq(argument: Int, value: String) : ConstantCondition(argument, value) {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        return UtTrue // TODO
    }
}

class ConstantGt(argument: Int, value: String) : ConstantCondition(argument, value) {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        return UtTrue // TODO
    }
}

class ConstantLt(argument: Int, value: String) : ConstantCondition(argument, value) {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        return UtTrue // TODO
    }
}

//class ParametersInclude(argument: Int, value: String) : LiteralArrayConstantCondition
//
//class ParameterStartsWith(argument: Int, value: String) : LiteralArrayConstantCondition

class IsType(
    argument: Int,
    packageName: NameInformation,
    className: NameInformation
) : AbstractTypeCondition(argument, packageName, className) {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        val elementForProcessing = when (argument) {
            -1 -> returnValue
            0 -> base
            else -> args.getOrNull(argument - 1) ?: return UtTrue // TODO is it right?
        }

        return mkTrue() // TODO
    }

    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        TODO("Not yet implemented")
    }
}

// TODO what is it????
class AnnotationType(
    argument: Int,
    packageName: NameInformation,
    className: NameInformation
) : AbstractTypeCondition(argument, packageName, className) {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        return UtTrue
    }

    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        TODO("Not yet implemented")
    }
}

class ConstantMatches : Condition {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        return UtTrue // TODO not implemented
    }

    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        TODO("Not yet implemented")
    }
}

class SourceFunctionMatches : Condition {
    override fun toBoolExpr(
        traversalContext: TraversalContext,
        taintValue: PrimitiveValue,
        base: SymbolicValue?,
        args: List<SymbolicValue>,
        returnValue: SymbolicValue?
    ): UtBoolExpression {
        return UtTrue // TODO not implemented
    }

    override fun collectRelatedTaintKinds(taintKinds: TaintKinds) {
        TODO("Not yet implemented")
    }
}

private fun isArgumentValid(invokeExpr: InvokeExpr, argument: Int): Boolean {
    val argCount = invokeExpr.argCount
    // if there is no arguments in the invokeExpr
    if (argCount == 0) {
        return false
    }

    // if the argument is `this` instance
    if (argument == 0) {
        return false
    }

    return argument <= argCount
}