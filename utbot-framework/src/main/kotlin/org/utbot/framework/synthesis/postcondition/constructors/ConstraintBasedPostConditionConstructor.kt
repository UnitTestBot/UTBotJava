package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.*
import org.utbot.engine.pc.*
import org.utbot.engine.selectors.strategies.ConstraintScoringStrategyBuilder
import org.utbot.engine.selectors.strategies.ScoringStrategyBuilder
import org.utbot.engine.symbolic.SymbolicStateUpdate
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.symbolic.asSoftConstraint
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.UtConstraintParameter
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.synthesis.SynthesisMethodContext
import org.utbot.framework.synthesis.SynthesisUnitContext
import org.utbot.framework.synthesis.toSoot
import org.utbot.framework.synthesis.toSootType
import soot.ArrayType
import soot.RefType


class ConstraintBasedPostConditionConstructor(
    private val models: List<UtModel>,
    private val unitContext: SynthesisUnitContext,
    private val methodContext: SynthesisMethodContext
) : PostConditionConstructor {

    override fun constructPostCondition(
        traverser: Traverser,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate = UtConstraint2ExpressionConverter(traverser).run {
        var constraints = SymbolicStateUpdate()
        val entryFrame = traverser.environment.state.executionStack.first()
        val frameParameters = entryFrame.parameters.map { it.value }
        for (model in models) {
            constraints += buildPostCondition(
                model,
                this,
                frameParameters,
                traverser.environment.state.localVariableMemory
            ).asHardConstraint()
        }
        constraints
    }

    override fun constructSoftPostCondition(
        traverser: Traverser,
    ): SymbolicStateUpdate = UtConstraint2ExpressionConverter(
        traverser
    ).run {
        var constraints = SymbolicStateUpdate()
        val entryFrame = traverser.environment.state.executionStack.first()
        val frameParameters = entryFrame.parameters.map { it.value }
        for (model in models) {
            constraints += buildPostCondition(
                model,
                this,
                frameParameters,
                traverser.environment.state.localVariableMemory
            ).asSoftConstraint()
        }
        constraints
    }

    override fun scoringBuilder(): ScoringStrategyBuilder {
        return ConstraintScoringStrategyBuilder(
            models,
            unitContext,
            methodContext,
        )
    }

    private fun buildPostCondition(
        model: UtModel,
        builder: UtConstraint2ExpressionConverter,
        parameters: List<SymbolicValue>,
        localVariableMemory: LocalVariableMemory,
    ): Set<UtBoolExpression> = buildSet {
        val modelUnit = unitContext[model]
        val symbolicValue = when {
            model.classId.isPrimitive -> parameters[methodContext.unitToParameter[modelUnit]!!.number]
            else -> localVariableMemory.local(methodContext.unitToLocal[modelUnit]!!.variable)
                ?: return@buildSet
        }
        when (model) {
            is UtNullModel -> {
                add(mkEq(symbolicValue.addr, nullObjectAddr))
            }

            is UtConstraintModel -> {
                if (model is UtElementContainerConstraintModel) {
                    addAll(buildPostCondition(model.length, builder, parameters, localVariableMemory))
                    for ((index, element) in model.elements) {
                        addAll(buildPostCondition(index, builder, parameters, localVariableMemory))
                        addAll(buildPostCondition(element, builder, parameters, localVariableMemory))
                    }
                }
                for (constraint in model.utConstraints) {
                    add(constraint.accept(builder))
                }
                add(mkEq(symbolicValue, model.variable.accept(builder)))
            }

            else -> error("Unknown model: ${model::class}")
        }
    }
}

class UtConstraint2ExpressionConverter(
    private val traverser: Traverser
) : UtConstraintVisitor<UtBoolExpression>, UtConstraintVariableVisitor<SymbolicValue> {
    override fun visitUtConstraintParameter(expr: UtConstraintParameter): SymbolicValue = with(expr) {
        when {
            isPrimitive -> {
                val newName = "post_condition_$name"
                when (classId) {
                    voidClassId -> voidValue
                    booleanClassId -> mkBoolConst(newName).toBoolValue()
                    byteClassId -> mkBVConst(newName, UtByteSort).toByteValue()
                    shortClassId -> mkBVConst(newName, UtShortSort).toShortValue()
                    charClassId -> mkBVConst(newName, UtCharSort).toCharValue()
                    intClassId -> mkBVConst(newName, UtIntSort).toIntValue()
                    longClassId -> mkBVConst(newName, UtLongSort).toLongValue()
                    floatClassId -> mkFpConst(newName, Float.SIZE_BITS).toFloatValue()
                    doubleClassId -> mkFpConst(newName, Double.SIZE_BITS).toDoubleValue()
                    else -> error("Unknown primitive parameter: $this")
                }
            }

            isArray -> {
                val sootType = classId.toSootType() as ArrayType
                val addr = UtAddrExpression(mkBVConst("post_condition_${name}", UtIntSort))
                traverser.createArray(addr, sootType, useConcreteType = addr.isThisAddr)
            }

            else -> {
                val sootType = classId.toSoot().type
                val addr = UtAddrExpression(mkBVConst("post_condition_${name}", UtIntSort))
                traverser.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
            }
        }
    }

    override fun visitUtConstraintNull(expr: UtConstraintNull): SymbolicValue = with(expr) {
        when {
            classId.isArray -> traverser.createArray(nullObjectAddr, classId.toSootType() as ArrayType)
            else -> traverser.createObject(
                nullObjectAddr,
                classId.toSoot().type,
                mockInfoGenerator = null,
                useConcreteType = false
            )
        }
    }

    override fun visitUtConstraintFieldAccess(expr: UtConstraintFieldAccess): SymbolicValue = with(expr) {
        val sootField = fieldId.declaringClass.toSoot().getFieldByName(fieldId.name)
        val type = sootField.declaringClass.type
        val instanceVal = instance.accept(this@UtConstraint2ExpressionConverter)
        try {
            traverser.createFieldOrMock(
                type,
                instanceVal.addr,
                sootField,
                mockInfoGenerator = null
            )
        } catch (e: Throwable) {
            throw e
        }
    }

    override fun visitUtConstraintArrayAccess(expr: UtConstraintArrayAccess): SymbolicValue = with(expr) {
        val arrayInstance = instance.accept(this@UtConstraint2ExpressionConverter)
        val index = index.accept(this@UtConstraint2ExpressionConverter)
        val type = instance.classId.toSootType() as? ArrayType ?: ArrayType.v(OBJECT_TYPE.sootClass.type, 1)
        val elementType = type.elementType
        val chunkId = traverser.typeRegistry.arrayChunkId(type)
        val descriptor = MemoryChunkDescriptor(chunkId, type, elementType).also { traverser.touchMemoryChunk(it) }
        val array = traverser.memory.findArray(descriptor)

        when (elementType) {
            is RefType -> {
                val generator = UtMockInfoGenerator { mockAddr -> UtObjectMockInfo(elementType.id, mockAddr) }

                val objectValue = traverser.createObject(
                    UtAddrExpression(array.select(arrayInstance.addr, index.exprValue)),
                    elementType,
                    useConcreteType = false,
                    generator
                )

                if (objectValue.type.isJavaLangObject()) {
                    traverser.queuedSymbolicStateUpdates += traverser.typeRegistry.zeroDimensionConstraint(objectValue.addr)
                        .asSoftConstraint()
                }

                objectValue
            }

            is ArrayType -> traverser.createArray(
                UtAddrExpression(array.select(arrayInstance.addr, index.exprValue)),
                elementType,
                useConcreteType = false
            )

            else -> PrimitiveValue(elementType, array.select(arrayInstance.addr, index.exprValue))
        }
    }

    override fun visitUtConstraintArrayLengthAccess(expr: UtConstraintArrayLength): SymbolicValue = with(expr) {
        val array = instance.accept(this@UtConstraint2ExpressionConverter)
        traverser.memory.findArrayLength(array.addr)
    }

    override fun visitUtConstraintBoolConstant(expr: UtConstraintBoolConstant): SymbolicValue =
        expr.value.toPrimitiveValue()

    override fun visitUtConstraintCharConstant(expr: UtConstraintCharConstant): SymbolicValue =
        expr.value.toPrimitiveValue()

    override fun visitUtConstraintNumericConstant(expr: UtConstraintNumericConstant): SymbolicValue =
        expr.value.primitiveToSymbolic()

    override fun visitUtConstraintAdd(expr: UtConstraintAdd): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Add(elhv, erhv)
        )
    }

    override fun visitUtConstraintAnd(expr: UtConstraintAnd): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            And(elhv, erhv)
        )
    }

    override fun visitUtConstraintCmp(expr: UtConstraintCmp): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Cmp(elhv, erhv)
        )
    }

    override fun visitUtConstraintCmpg(expr: UtConstraintCmpg): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Cmpg(elhv, erhv)
        )
    }

    override fun visitUtConstraintCmpl(expr: UtConstraintCmpl): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Cmpl(elhv, erhv)
        )
    }

    override fun visitUtConstraintDiv(expr: UtConstraintDiv): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Div(elhv, erhv)
        )
    }

    override fun visitUtConstraintMul(expr: UtConstraintMul): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Mul(elhv, erhv)
        )
    }

    override fun visitUtConstraintOr(expr: UtConstraintOr): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Or(elhv, erhv)
        )
    }

    override fun visitUtConstraintRem(expr: UtConstraintRem): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Rem(elhv, erhv)
        )
    }

    override fun visitUtConstraintShl(expr: UtConstraintShl): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Shl(elhv, erhv)
        )
    }

    override fun visitUtConstraintShr(expr: UtConstraintShr): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Shr(elhv, erhv)
        )
    }

    override fun visitUtConstraintSub(expr: UtConstraintSub): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Sub(elhv, erhv)
        )
    }

    override fun visitUtConstraintUshr(expr: UtConstraintUshr): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Ushr(elhv, erhv)
        )
    }

    override fun visitUtConstraintXor(expr: UtConstraintXor): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Xor(elhv, erhv)
        )
    }

    override fun visitUtConstraintNot(expr: UtConstraintNot): SymbolicValue = with(expr) {
        val oper = operand.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(oper.type, mkNot(oper.expr as UtBoolExpression))
    }

    override fun visitUtConstraintNeg(expr: UtConstraintNeg): SymbolicValue = with(expr) {
        val oper = operand.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        PrimitiveValue(
            oper.type, UtNegExpression(oper)
        )
    }

    override fun visitUtConstraintCast(expr: UtConstraintCast): SymbolicValue = with(expr) {
        val oper = operand.accept(this@UtConstraint2ExpressionConverter) as? PrimitiveValue
            ?: error("a")
        PrimitiveValue(
            oper.type, UtCastExpression(oper, classId.toSootType())
        )
    }

    override fun visitUtNegatedConstraint(expr: UtNegatedConstraint): UtBoolExpression = with(expr) {
        mkNot(constraint.accept(this@UtConstraint2ExpressionConverter))
    }

    override fun visitUtRefEqConstraint(expr: UtRefEqConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraint2ExpressionConverter)
        val rhvVal = rhv.accept(this@UtConstraint2ExpressionConverter)
        mkEq(lhvVal, rhvVal)
    }

    override fun visitUtRefGenericEqConstraint(expr: UtRefGenericEqConstraint) = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraint2ExpressionConverter)
        val rhvVal = rhv.accept(this@UtConstraint2ExpressionConverter)
        UtEqGenericTypeParametersExpression(lhvVal.addr, rhvVal.addr, mapping)
    }

    override fun visitUtRefTypeConstraint(expr: UtRefTypeConstraint): UtBoolExpression = with(expr) {
        val lhvVal = operand.accept(this@UtConstraint2ExpressionConverter)
        val type = type.toSootType()
        traverser.typeRegistry
            .typeConstraint(
                lhvVal.addr,
                traverser.typeResolver.constructTypeStorage(type, false)
            )
            .isConstraint()
    }

    override fun visitUtRefGenericTypeConstraint(expr: UtRefGenericTypeConstraint): UtBoolExpression = with(expr) {
        val operandVal = operand.accept(this@UtConstraint2ExpressionConverter)
        val baseVal = base.accept(this@UtConstraint2ExpressionConverter)
        UtIsGenericTypeExpression(operandVal.addr, baseVal.addr, parameterIndex)
    }

    override fun visitUtBoolConstraint(expr: UtBoolConstraint): UtBoolExpression =
        expr.operand.accept(this@UtConstraint2ExpressionConverter).exprValue as UtBoolExpression

    override fun visitUtEqConstraint(expr: UtEqConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraint2ExpressionConverter)
        val rhvVal = rhv.accept(this@UtConstraint2ExpressionConverter)
        mkEq(lhvVal, rhvVal)
    }

    override fun visitUtLtConstraint(expr: UtLtConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val rhvVal = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        Lt(lhvVal, rhvVal)
    }

    override fun visitUtGtConstraint(expr: UtGtConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val rhvVal = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        Gt(lhvVal, rhvVal)
    }

    override fun visitUtLeConstraint(expr: UtLeConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val rhvVal = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        Le(lhvVal, rhvVal)
    }

    override fun visitUtGeConstraint(expr: UtGeConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        val rhvVal = rhv.accept(this@UtConstraint2ExpressionConverter) as PrimitiveValue
        Ge(lhvVal, rhvVal)
    }

    override fun visitUtAndConstraint(expr: UtAndConstraint): UtBoolExpression = mkAnd(
        expr.lhv.accept(this),
        expr.rhv.accept(this)
    )

    override fun visitUtOrConstraint(expr: UtOrConstraint): UtBoolExpression = mkOr(
        expr.lhv.accept(this),
        expr.rhv.accept(this)
    )

    private val SymbolicValue.exprValue
        get() = when (this) {
            is ReferenceValue -> addr
            is PrimitiveValue -> expr
        }
}

