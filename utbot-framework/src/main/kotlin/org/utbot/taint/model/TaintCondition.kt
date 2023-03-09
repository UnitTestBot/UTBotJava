package org.utbot.taint.model

import kotlinx.serialization.Serializable
import org.utbot.engine.*
import org.utbot.engine.pc.*
import soot.Scene

/**
 * Condition that imposed on the taint rule. It must be met to trigger the rule.
 */
@Serializable
sealed interface TaintCondition {
    fun toBoolExpr(traverser: Traverser, methodData: SymbolicMethodData): UtBoolExpression
}

/**
 * Returns always true.
 */
@Serializable
object ConditionTrue : TaintCondition {
    override fun toBoolExpr(traverser: Traverser, methodData: SymbolicMethodData): UtBoolExpression =
        UtTrue
}

/**
 * The [entity] must be equal to [argumentValue].
 */
@Serializable
class ConditionEqualValue(
    private val entity: TaintEntity,
    private val argumentValue: ArgumentValue
) : TaintCondition {
    override fun toBoolExpr(traverser: Traverser, methodData: SymbolicMethodData): UtBoolExpression {
        val symbolicValue = methodData.choose(entity) ?:
            return UtFalse
        // TODO: support java.lang.Boolean, java.lang.Integer, etc.
        return when (argumentValue) {
            ArgumentValueNull -> {
                val referenceValue = symbolicValue as? ReferenceValue
                    ?: return UtFalse
                addrEq(referenceValue.addr, nullObjectAddr)
            }
            is ArgumentValueBoolean -> {
                val primitiveValue = symbolicValue as? PrimitiveValue
                    ?: return UtFalse
                mkEq(primitiveValue, mkBool(argumentValue.value).toBoolValue())
            }
            is ArgumentValueLong -> {
                val primitiveValue = symbolicValue as? PrimitiveValue
                    ?: return UtFalse
                // conversions like int -> long will be made automatically by utbot
                mkEq(primitiveValue, mkLong(argumentValue.value).toLongValue())
            }
            is ArgumentValueDouble -> {
                val primitiveValue = symbolicValue as? PrimitiveValue
                    ?: return UtFalse
                // conversion float -> double will be made automatically by utbot
                mkEq(primitiveValue, mkDouble(argumentValue.value).toDoubleValue())
            }
            is ArgumentValueString -> {
                val objectValue = symbolicValue as? ObjectValue
                    ?: return UtFalse
                // TODO: compare not only length
                val symbolicLength = traverser.getIntFieldValue(objectValue, STRING_LENGTH)
                mkEq(symbolicLength, mkInt(argumentValue.value.length))
            }
        }
    }
}

/**
 * The [entity] must be [argumentType] at the runtime.
 */
@Serializable
class ConditionIsType(
    private val entity: TaintEntity,
    private val argumentType: ArgumentType
) : TaintCondition {
    override fun toBoolExpr(traverser: Traverser, methodData: SymbolicMethodData): UtBoolExpression {
        val symbolicValue = methodData.choose(entity) ?:
            return UtFalse
        return when (argumentType) {
            ArgumentTypeAny -> UtTrue
            is ArgumentTypeString -> {
                when (symbolicValue) {
                    is PrimitiveValue -> {
                        // If the method receives <long> and the user calls it with the <int> argument,
                        // utbot will automatically create a symbolic value with the <long> type,
                        // so there is no need to handle conversions like int -> long here.
                        val argumentType = Scene.v().getTypeUnsafe(argumentType.typeFqn)
                        mkBool(symbolicValue.type == argumentType)
                    }
                    is ReferenceValue -> {
                        val argumentRefType = Scene.v().getRefTypeUnsafe(argumentType.typeFqn)
                            ?: return UtFalse
                        val typeStorage = traverser.typeResolver.constructTypeStorage(argumentRefType, useConcreteType = false)
                        traverser.typeRegistry.typeConstraint(symbolicValue.addr, typeStorage).isConstraint()
                    }
                }
            }
        }
    }
}

/**
 * Negates [inner].
 */
@Serializable
class ConditionNot(private val inner: TaintCondition) : TaintCondition {
    override fun toBoolExpr(traverser: Traverser, methodData: SymbolicMethodData): UtBoolExpression =
        mkNot(inner.toBoolExpr(traverser, methodData))
}

/**
 * Combines [inners] with `or` operator.
 */
@Serializable
class ConditionOr(private val inners: List<TaintCondition>) : TaintCondition {
    override fun toBoolExpr(traverser: Traverser, methodData: SymbolicMethodData): UtBoolExpression =
        mkOr(inners.map { it.toBoolExpr(traverser, methodData) })
}

/**
 * Combines [inners] with `and` operator.
 */
@Serializable
class ConditionAnd(private val inners: List<TaintCondition>) : TaintCondition {
    override fun toBoolExpr(traverser: Traverser, methodData: SymbolicMethodData): UtBoolExpression =
        mkAnd(inners.map { it.toBoolExpr(traverser, methodData) })
}
