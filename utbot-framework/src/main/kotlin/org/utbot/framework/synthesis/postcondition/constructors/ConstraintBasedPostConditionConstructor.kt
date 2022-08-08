package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.*
import org.utbot.engine.pc.*
import org.utbot.engine.symbolic.SymbolicStateUpdate
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.symbolic.asSoftConstraint
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.UtConstraintParameter
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.synthesis.SynthesisMethodContext
import org.utbot.framework.synthesis.SynthesisUnitContext
import soot.ArrayType
import soot.RefType


class ConstraintBasedPostConditionConstructor(
    private val models: List<UtModel>,
    private val unitContext: SynthesisUnitContext,
    private val methodContext: SynthesisMethodContext
) : PostConditionConstructor {

    override fun constructPostCondition(
        engine: UtBotSymbolicEngine,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate = UtConstraintBuilder(engine).run {
        var constraints = SymbolicStateUpdate()
        val entryFrame = engine.environment.state.executionStack.first()
        val frameParameters = entryFrame.parameters.map { it.value }
        for (model in models) {
            constraints += buildPostCondition(
                model,
                this,
                frameParameters,
                engine.environment.state.localVariableMemory
            ).asHardConstraint()
        }
        constraints
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun buildPostCondition(
        model: UtModel,
        builder: UtConstraintBuilder,
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
                if (model is UtArrayConstraintModel) {
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

    override fun constructSoftPostCondition(
        engine: UtBotSymbolicEngine
    ): SymbolicStateUpdate = UtConstraintBuilder(
        engine
    ).run {
        TODO()
//        for ((index, parameter) in models.parameters.withIndex()) {
//            val local = locals[index]
//            val sv = engine.environment.state.localVariableMemory.local(local)!!
//            when (parameter) {
//                is UtNullModel -> {
//                    constraints += mkEq(sv.addr, nullObjectAddr).asSoftConstraint()
//                }
//                is UtConstraintModel -> {
//                    for (constraint in parameter.utConstraints) {
//                        buildConstraint(constraint)
//                        constraints += mkEq(sv, buildExpression(parameter.variable)).asSoftConstraint()
//                    }
//
//                }
//                else -> error("Unknown model: ${parameter::class}")
//            }
//        }
//        constraints
    }
}

private class UtConstraintBuilder(
    private val engine: UtBotSymbolicEngine
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
                engine.createArray(addr, sootType, useConcreteType = addr.isThisAddr)
            }
            else -> {
                val sootType = classId.toSoot().type
                val addr = UtAddrExpression(mkBVConst("post_condition_${name}", UtIntSort))
                engine.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
            }
        }
    }

    override fun visitUtConstraintNull(expr: UtConstraintNull): SymbolicValue = with(expr) {
        when {
            classId.isArray -> engine.createArray(nullObjectAddr, classId.toSootType() as ArrayType)
            else -> engine.createObject(
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
        val instanceVal = instance.accept(this@UtConstraintBuilder)
        try {
            engine.createFieldOrMock(
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
        val arrayInstance = instance.accept(this@UtConstraintBuilder)
        val index = index.accept(this@UtConstraintBuilder)
        val type = instance.classId.toSootType() as? ArrayType ?: ArrayType.v(OBJECT_TYPE.sootClass.type, 1)
        val elementType = type.elementType
        val chunkId = engine.typeRegistry.arrayChunkId(type)
        val descriptor = MemoryChunkDescriptor(chunkId, type, elementType).also { engine.touchMemoryChunk(it) }
        val array = engine.memory.findArray(descriptor)

        when (elementType) {
            is RefType -> {
                val generator = UtMockInfoGenerator { mockAddr -> UtObjectMockInfo(elementType.id, mockAddr) }

                val objectValue = engine.createObject(
                    UtAddrExpression(array.select(arrayInstance.addr, index.exprValue)),
                    elementType,
                    useConcreteType = false,
                    generator
                )

                if (objectValue.type.isJavaLangObject()) {
                    engine.queuedSymbolicStateUpdates += engine.typeRegistry.zeroDimensionConstraint(objectValue.addr)
                        .asSoftConstraint()
                }

                objectValue
            }
            is ArrayType -> engine.createArray(
                UtAddrExpression(array.select(arrayInstance.addr, index.exprValue)),
                elementType,
                useConcreteType = false
            )
            else -> PrimitiveValue(elementType, array.select(arrayInstance.addr, index.exprValue))
        }
    }

    override fun visitUtConstraintArrayLengthAccess(expr: UtConstraintArrayLength): SymbolicValue = with(expr) {
        val array = instance.accept(this@UtConstraintBuilder)
        engine.memory.findArrayLength(array.addr)
    }

    override fun visitUtConstraintBoolConstant(expr: UtConstraintBoolConstant): SymbolicValue =
        expr.value.toPrimitiveValue()

    override fun visitUtConstraintCharConstant(expr: UtConstraintCharConstant): SymbolicValue =
        expr.value.toPrimitiveValue()

    override fun visitUtConstraintNumericConstant(expr: UtConstraintNumericConstant): SymbolicValue =
        expr.value.primitiveToSymbolic()

    override fun visitUtConstraintAdd(expr: UtConstraintAdd): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Add(elhv, erhv)
        )
    }

    override fun visitUtConstraintAnd(expr: UtConstraintAnd): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            And(elhv, erhv)
        )
    }

    override fun visitUtConstraintCmp(expr: UtConstraintCmp): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Cmp(elhv, erhv)
        )
    }

    override fun visitUtConstraintCmpg(expr: UtConstraintCmpg): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Cmpg(elhv, erhv)
        )
    }

    override fun visitUtConstraintCmpl(expr: UtConstraintCmpl): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Cmpl(elhv, erhv)
        )
    }

    override fun visitUtConstraintDiv(expr: UtConstraintDiv): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Div(elhv, erhv)
        )
    }

    override fun visitUtConstraintMul(expr: UtConstraintMul): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Mul(elhv, erhv)
        )
    }

    override fun visitUtConstraintOr(expr: UtConstraintOr): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Or(elhv, erhv)
        )
    }

    override fun visitUtConstraintRem(expr: UtConstraintRem): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Rem(elhv, erhv)
        )
    }

    override fun visitUtConstraintShl(expr: UtConstraintShl): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Shl(elhv, erhv)
        )
    }

    override fun visitUtConstraintShr(expr: UtConstraintShr): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Shr(elhv, erhv)
        )
    }

    override fun visitUtConstraintSub(expr: UtConstraintSub): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Sub(elhv, erhv)
        )
    }

    override fun visitUtConstraintUshr(expr: UtConstraintUshr): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Ushr(elhv, erhv)
        )
    }

    override fun visitUtConstraintXor(expr: UtConstraintXor): SymbolicValue = with(expr) {
        val elhv = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val erhv = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            classId.toSootType(),
            Xor(elhv, erhv)
        )
    }

    override fun visitUtConstraintNot(expr: UtConstraintNot): SymbolicValue = with(expr) {
        val oper = operand.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(oper.type, mkNot(oper.expr as UtBoolExpression))
    }

    override fun visitUtConstraintNeg(expr: UtConstraintNeg): SymbolicValue = with(expr) {
        val oper = operand.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            oper.type, UtNegExpression(oper)
        )
    }

    override fun visitUtConstraintCast(expr: UtConstraintCast): SymbolicValue = with(expr) {
        val oper = operand.accept(this@UtConstraintBuilder) as PrimitiveValue
        PrimitiveValue(
            oper.type, UtCastExpression(oper, classId.toSootType())
        )
    }

    override fun visitUtNegatedConstraint(expr: UtNegatedConstraint): UtBoolExpression = with(expr) {
        mkNot(constraint.accept(this@UtConstraintBuilder))
    }

    override fun visitUtRefEqConstraint(expr: UtRefEqConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraintBuilder)
        val rhvVal = rhv.accept(this@UtConstraintBuilder)
        mkEq(lhvVal, rhvVal)
    }

    override fun visitUtRefGenericEqConstraint(expr: UtRefGenericEqConstraint) = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraintBuilder)
        val rhvVal = rhv.accept(this@UtConstraintBuilder)
        UtEqGenericTypeParametersExpression(lhvVal.addr, rhvVal.addr, mapping)
    }

    override fun visitUtRefTypeConstraint(expr: UtRefTypeConstraint): UtBoolExpression = with(expr) {
        val lhvVal = operand.accept(this@UtConstraintBuilder)
        val type = type.toSootType()
        engine.typeRegistry.typeConstraint(lhvVal.addr, TypeStorage(type)).isConstraint()
    }

    override fun visitUtRefGenericTypeConstraint(expr: UtRefGenericTypeConstraint): UtBoolExpression = with(expr) {
        val operandVal = operand.accept(this@UtConstraintBuilder)
        val baseVal = base.accept(this@UtConstraintBuilder)
        UtIsGenericTypeExpression(operandVal.addr, baseVal.addr, parameterIndex)
    }

    override fun visitUtBoolConstraint(expr: UtBoolConstraint): UtBoolExpression =
        expr.operand.accept(this@UtConstraintBuilder).exprValue as UtBoolExpression

    override fun visitUtEqConstraint(expr: UtEqConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraintBuilder)
        val rhvVal = rhv.accept(this@UtConstraintBuilder)
        mkEq(lhvVal, rhvVal)
    }

    override fun visitUtLtConstraint(expr: UtLtConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val rhvVal = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        Lt(lhvVal, rhvVal)
    }

    override fun visitUtGtConstraint(expr: UtGtConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val rhvVal = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        Gt(lhvVal, rhvVal)
    }

    override fun visitUtLeConstraint(expr: UtLeConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val rhvVal = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        Le(lhvVal, rhvVal)
    }

    override fun visitUtGeConstraint(expr: UtGeConstraint): UtBoolExpression = with(expr) {
        val lhvVal = lhv.accept(this@UtConstraintBuilder) as PrimitiveValue
        val rhvVal = rhv.accept(this@UtConstraintBuilder) as PrimitiveValue
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

