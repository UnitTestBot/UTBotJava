package org.utbot.fuzzer

import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId
import mu.KotlinLogging
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.ShortType
import soot.Unit
import soot.Value
import soot.ValueBox
import soot.jimple.Constant
import soot.jimple.InvokeExpr
import soot.jimple.NullConstant
import soot.jimple.internal.ImmediateBox
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JCastExpr
import soot.jimple.internal.JEqExpr
import soot.jimple.internal.JGeExpr
import soot.jimple.internal.JGtExpr
import soot.jimple.internal.JIfStmt
import soot.jimple.internal.JLeExpr
import soot.jimple.internal.JLtExpr
import soot.jimple.internal.JNeExpr
import soot.jimple.internal.JVirtualInvokeExpr
import soot.toolkits.graph.ExceptionalUnitGraph

private val logger = KotlinLogging.logger {}

/**
 * Finds constant values in method body.
 */
fun collectConstantsForFuzzer(graph: ExceptionalUnitGraph): Set<FuzzedConcreteValue> {
    return graph.body.units.reversed().asSequence()
        .filter { it is JIfStmt || it is JAssignStmt }
        .flatMap { unit ->
            unit.useBoxes.map { unit to it.value }
        }
        .filter { (_, value) ->
            value is Constant || value is JCastExpr || value is InvokeExpr
        }
        .flatMap { (unit, value) ->
            sequenceOf(
                ConstantsFromIfStatement,
                ConstantsFromCast,
                BoundValuesForDoubleChecks,
                StringConstant,
            ).flatMap { finder ->
                try {
                    finder.find(graph, unit, value)
                } catch (e: Exception) {
                    logger.warn(e) { "Cannot process constant value of type '${value.type}}'" }
                    emptyList()
                }
            }.let { result ->
                if (result.any()) result else {
                    ConstantsAsIs.find(graph, unit, value).asSequence()
                }
            }
        }.toSet()
}

private interface ConstantsFinder {
    fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue>
}

private object ConstantsFromIfStatement: ConstantsFinder {
    override fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue> {
        if (value !is Constant || (unit !is JIfStmt && unit !is JAssignStmt)) return emptyList()

        var useBoxes: List<Value> = emptyList()
        var ifStatement: JIfStmt? = null
        // simple if statement
        if (unit is JIfStmt) {
            useBoxes = unit.conditionBox.value.useBoxes.mapNotNull { (it as? ImmediateBox)?.value }
            ifStatement = unit
        }
        // statement with double and long that consists of 2 units:
        // 1. compare (result = local compare constant)
        // 2. if result
        if (unit is JAssignStmt) {
            useBoxes = unit.rightOp.useBoxes.mapNotNull { (it as? ImmediateBox)?.value }
            ifStatement = nextDirectUnit(graph, unit) as? JIfStmt
        }

        /*
         * It is acceptable to check different types in if statement like ```2 == 2L```.
         *
         * Because of that fuzzer tries to find out the correct type between local and constant.
         * Constant should be converted into type of local var in such way that if-statement can be true.
         */
        val valueIndex = useBoxes.indexOf(value)
        if (useBoxes.size == 2 && valueIndex >= 0 && ifStatement != null) {
            val exactValue = value.plainValue
            val local = useBoxes[(valueIndex + 1) % 2]
            var op = sootIfToFuzzedOp(ifStatement)
            if (valueIndex == 0) {
                op = reverse(op)
            }
            // Soot loads any integer type as an Int,
            // therefore we try to guess target type using second value
            // in the if statement
            return listOfNotNull(
                when (local.type) {
                    is CharType -> FuzzedConcreteValue(charClassId, (exactValue as Int).toChar(), op)
                    is BooleanType -> FuzzedConcreteValue(booleanClassId, (exactValue == 1), op)
                    is ByteType -> FuzzedConcreteValue(byteClassId, (exactValue as Int).toByte(), op)
                    is ShortType -> FuzzedConcreteValue(shortClassId, (exactValue as Int).toShort(), op)
                    is IntType -> FuzzedConcreteValue(intClassId, exactValue, op)
                    is LongType -> FuzzedConcreteValue(longClassId, exactValue, op)
                    is FloatType -> FuzzedConcreteValue(floatClassId, exactValue, op)
                    is DoubleType -> FuzzedConcreteValue(doubleClassId, exactValue, op)
                    else -> null
                }
            )
        }
        return emptyList()
    }

}

private object ConstantsFromCast: ConstantsFinder {
    override fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue> {
        if (value !is JCastExpr) return emptyList()

        val next = nextDirectUnit(graph, unit)
        if (next is JAssignStmt) {
            val const = next.useBoxes.findFirstInstanceOf<Constant>()
            if (const != null) {
                val op = (nextDirectUnit(graph, next) as? JIfStmt)?.let(::sootIfToFuzzedOp) ?: FuzzedOp.NONE
                val exactValue = const.plainValue as Number
                return listOfNotNull(
                    when (value.op.type) {
                        is ByteType -> FuzzedConcreteValue(byteClassId, exactValue.toByte(), op)
                        is ShortType -> FuzzedConcreteValue(shortClassId, exactValue.toShort(), op)
                        is IntType -> FuzzedConcreteValue(intClassId, exactValue.toInt(), op)
                        is FloatType -> FuzzedConcreteValue(floatClassId, exactValue.toFloat(), op)
                        else -> null
                    }
                )
            }
        }
        return emptyList()
    }

}

private object BoundValuesForDoubleChecks: ConstantsFinder {
    override fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue> {
        if (value !is InvokeExpr) return emptyList()
        if (value.method.declaringClass.name != "java.lang.Double") return emptyList()
        return when (value.method.name) {
            "isNaN", "isInfinite", "isFinite" -> listOf(
                FuzzedConcreteValue(doubleClassId, Double.POSITIVE_INFINITY),
                FuzzedConcreteValue(doubleClassId, Double.NaN),
                FuzzedConcreteValue(doubleClassId, 0.0),
            )
            else -> emptyList()
        }
    }

}

private object StringConstant: ConstantsFinder {
    override fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue> {
        if (unit !is JAssignStmt || value !is JVirtualInvokeExpr) return emptyList()
        // if string constant is called from String class let's pass it as modification
        if (value.method.declaringClass.name == "java.lang.String") {
            val stringConstantWasPassedAsArg = unit.useBoxes.findFirstInstanceOf<Constant>()?.plainValue
            if (stringConstantWasPassedAsArg != null) {
                return listOf(FuzzedConcreteValue(stringClassId, stringConstantWasPassedAsArg, FuzzedOp.CH))
            }
            val stringConstantWasPassedAsThis = graph.getPredsOf(unit)
                ?.filterIsInstance<JAssignStmt>()
                ?.firstOrNull()
                ?.useBoxes
                ?.findFirstInstanceOf<Constant>()
                ?.plainValue
            if (stringConstantWasPassedAsThis != null) {
                return listOf(FuzzedConcreteValue(stringClassId, stringConstantWasPassedAsThis, FuzzedOp.CH))
            }
        }
        return emptyList()
    }

}

private object ConstantsAsIs: ConstantsFinder {
    override fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue> {
        if (value !is Constant || value is NullConstant) return emptyList()
        return listOf(FuzzedConcreteValue(value.type.classId, value.plainValue))

    }

}

private inline fun <reified T>  List<ValueBox>.findFirstInstanceOf(): T? {
    return map { it.value }
        .filterIsInstance<T>()
        .firstOrNull()
}

private val Constant.plainValue
    get() = javaClass.getField("value")[this]

private fun sootIfToFuzzedOp(unit: JIfStmt) = when (unit.condition) {
    is JEqExpr -> FuzzedOp.NE
    is JNeExpr -> FuzzedOp.EQ
    is JGtExpr -> FuzzedOp.LE
    is JGeExpr -> FuzzedOp.LT
    is JLtExpr -> FuzzedOp.GE
    is JLeExpr -> FuzzedOp.GT
    else -> FuzzedOp.NONE
}

private fun reverse(op: FuzzedOp) = when(op) {
    FuzzedOp.GT -> FuzzedOp.LT
    FuzzedOp.LT -> FuzzedOp.GT
    FuzzedOp.LE -> FuzzedOp.GE
    FuzzedOp.GE -> FuzzedOp.LE
    else -> op
}

private fun nextDirectUnit(graph: ExceptionalUnitGraph, unit: Unit): Unit? = graph.getSuccsOf(unit).takeIf { it.size == 1 }?.first()