package org.utbot.fuzzer

import mu.KotlinLogging
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.util.executableId
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.Local
import soot.LongType
import soot.ShortType
import soot.Unit
import soot.Value
import soot.ValueBox
import soot.jimple.Constant
import soot.jimple.IntConstant
import soot.jimple.InvokeExpr
import soot.jimple.NullConstant
import soot.jimple.internal.AbstractSwitchStmt
import soot.jimple.internal.ImmediateBox
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JCastExpr
import soot.jimple.internal.JEqExpr
import soot.jimple.internal.JGeExpr
import soot.jimple.internal.JGtExpr
import soot.jimple.internal.JIfStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JLeExpr
import soot.jimple.internal.JLookupSwitchStmt
import soot.jimple.internal.JLtExpr
import soot.jimple.internal.JNeExpr
import soot.jimple.internal.JSpecialInvokeExpr
import soot.jimple.internal.JStaticInvokeExpr
import soot.jimple.internal.JTableSwitchStmt
import soot.jimple.internal.JVirtualInvokeExpr
import soot.toolkits.graph.ExceptionalUnitGraph
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

private val logger = KotlinLogging.logger {}

/**
 * Finds constant values in method body.
 */
fun collectConstantsForFuzzer(graph: ExceptionalUnitGraph): Set<FuzzedConcreteValue> {
    return graph.body.units.reversed().asSequence()
        .filter { it is JIfStmt || it is JAssignStmt || it is AbstractSwitchStmt || it is JInvokeStmt }
        .flatMap { unit ->
            unit.useBoxes.map { unit to it.value }
        }
        .filter { (_, value) ->
            value is Constant || value is Local || value is JCastExpr || value is InvokeExpr
        }
        .flatMap { (unit, value) ->
            sequenceOf(
                ConstantsFromIfStatement,
                ConstantsFromCast,
                ConstantsFromSwitchCase,
                BoundValuesForDoubleChecks,
                StringConstant,
                RegexByVarStringConstant,
                DateFormatByVarStringConstant,
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
            if (valueIndex == 0 && op is FuzzedContext.Comparison) {
                op = op.reverse()
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
                val op = (nextDirectUnit(graph, next) as? JIfStmt)?.let(::sootIfToFuzzedOp) ?: FuzzedContext.Unknown
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

private object ConstantsFromSwitchCase: ConstantsFinder {
    override fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue> {
        if (unit !is JTableSwitchStmt && unit !is JLookupSwitchStmt) return emptyList()
        val result = mutableListOf<FuzzedConcreteValue>()
        if (unit is JTableSwitchStmt) {
            for (i in unit.lowIndex..unit.highIndex) {
                result.add(FuzzedConcreteValue(intClassId, i, FuzzedContext.Comparison.EQ))
            }
        }
        if (unit is JLookupSwitchStmt) {
            unit.lookupValues.asSequence().filterIsInstance<IntConstant>().forEach {
                result.add(FuzzedConcreteValue(intClassId, it.value, FuzzedContext.Comparison.EQ))
            }
        }
        return result
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
            if (stringConstantWasPassedAsArg != null && stringConstantWasPassedAsArg is String) {
                return listOf(FuzzedConcreteValue(stringClassId, stringConstantWasPassedAsArg, FuzzedContext.Call(value.method.executableId)))
            }
            val stringConstantWasPassedAsThis = graph.getPredsOf(unit)
                ?.filterIsInstance<JAssignStmt>()
                ?.firstOrNull()
                ?.useBoxes
                ?.findFirstInstanceOf<Constant>()
                ?.plainValue
            if (stringConstantWasPassedAsThis != null && stringConstantWasPassedAsThis is String) {
                return listOf(FuzzedConcreteValue(stringClassId, stringConstantWasPassedAsThis, FuzzedContext.Call(value.method.executableId)))
            }
        }
        return emptyList()
    }
}

/**
 * Finds strings that are used inside Pattern's methods.
 *
 * Due to compiler optimizations it should work when a string is assigned to a variable or static final field.
 */
private object RegexByVarStringConstant: ConstantsFinder {
    override fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue> {
        if (unit !is JAssignStmt || value !is JStaticInvokeExpr) return emptyList()
        if (value.method.declaringClass.name == "java.util.regex.Pattern") {
            val stringConstantWasPassedAsArg = unit.useBoxes.findFirstInstanceOf<Constant>()?.plainValue
            if (stringConstantWasPassedAsArg != null && stringConstantWasPassedAsArg is String) {
                return listOf(FuzzedConcreteValue(stringClassId, stringConstantWasPassedAsArg, FuzzedContext.Call(value.method.executableId)))
            }
        }
        return emptyList()
    }
}

/**
 * Finds strings that are used inside DateFormat's constructors.
 *
 * Due to compiler optimizations it should work when a string is assigned to a variable or static final field.
 */
private object DateFormatByVarStringConstant: ConstantsFinder {
    override fun find(graph: ExceptionalUnitGraph, unit: Unit, value: Value): List<FuzzedConcreteValue> {
        if (unit !is JInvokeStmt || value !is JSpecialInvokeExpr) return emptyList()
        if (value.method.isConstructor && value.method.declaringClass.name == "java.text.SimpleDateFormat") {
            val stringConstantWasPassedAsArg = unit.useBoxes.findFirstInstanceOf<Constant>()?.plainValue
            if (stringConstantWasPassedAsArg != null && stringConstantWasPassedAsArg is String) {
                return listOf(FuzzedConcreteValue(stringClassId, stringConstantWasPassedAsArg, FuzzedContext.Call(value.method.executableId)))
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
    is JEqExpr -> FuzzedContext.Comparison.NE
    is JNeExpr -> FuzzedContext.Comparison.EQ
    is JGtExpr -> FuzzedContext.Comparison.LE
    is JGeExpr -> FuzzedContext.Comparison.LT
    is JLtExpr -> FuzzedContext.Comparison.GE
    is JLeExpr -> FuzzedContext.Comparison.GT
    else -> FuzzedContext.Unknown
}

private fun nextDirectUnit(graph: ExceptionalUnitGraph, unit: Unit): Unit? = graph.getSuccsOf(unit).takeIf { it.size == 1 }?.first()

fun toFuzzerType(type: Type): FuzzedType {
    return when (type) {
        is WildcardType -> type.upperBounds.firstOrNull()?.let(::toFuzzerType) ?: FuzzedType(objectClassId)
        is TypeVariable<*> -> type.bounds.firstOrNull()?.let(::toFuzzerType) ?: FuzzedType(objectClassId)
        is ParameterizedType -> FuzzedType((type.rawType as Class<*>).id, type.actualTypeArguments.map { toFuzzerType(it) })
        is GenericArrayType -> {
            val genericComponentType = type.genericComponentType
            val fuzzerType = toFuzzerType(genericComponentType)
            val classId = if (genericComponentType !is GenericArrayType) {
                ClassId("[L${fuzzerType.classId.name};", fuzzerType.classId)
            } else {
                ClassId("[" + fuzzerType.classId.name, fuzzerType.classId)
            }
            FuzzedType(classId)
        }
        is Class<*> -> FuzzedType(type.id)
        else -> error("Unknown type: $type")
    }
}