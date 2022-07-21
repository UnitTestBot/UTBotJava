package org.utbot.framework.synthesis.postcondition.constructors

import com.microsoft.z3.ArrayExpr
import org.utbot.engine.*
import org.utbot.engine.pc.*
import org.utbot.engine.symbolic.SymbolicStateUpdate
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.symbolic.asSoftConstraint
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.UtConstraintParameter
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.synthesis.SynthesisParameter
import soot.ArrayType
import soot.RefType

class ConstraintBasedPostConditionConstructor(
    private val models: ResolvedModels,
    private val parameters: List<SynthesisParameter?>,
    private val locals: List<LocalVariable?>
) : PostConditionConstructor {

    override fun constructPostCondition(
        engine: UtBotSymbolicEngine,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate = UtConstraintBuilder(
        engine, soft = false
    ).run {
        val entryFrame = engine.environment.state.executionStack.first()
        val frameParameters = entryFrame.parameters.map { it.value }
        for ((index, model) in models.parameters.withIndex()) {
            val sv = when {
                model.classId.isPrimitive -> frameParameters[parameters[index]!!.number]
                else -> engine.environment.state.localVariableMemory.local(locals[index]!!) ?: continue
            }
            when (model) {
                is UtNullModel -> {
                    constraints += mkEq(sv.addr, nullObjectAddr).asHardConstraint()
                }
                is UtConstraintModel -> {
                    for (constraint in model.utConstraints) {
                        buildConstraint(constraint)
                        constraints += mkEq(sv, buildExpression(model.variable)).asHardConstraint()
                    }

                }
                else -> error("Unknown model: ${model::class}")
            }
        }
        constraints
    }

    override fun constructSoftPostCondition(
        engine: UtBotSymbolicEngine
    ): SymbolicStateUpdate = UtConstraintBuilder(
        engine, soft = true
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
    private val engine: UtBotSymbolicEngine,
    private val soft: Boolean = false
) {
    var constraints = SymbolicStateUpdate()

    private fun asConstraint(body: () -> UtBoolExpression) {
        when {
            soft -> constraints += body().asSoftConstraint()
            else -> constraints += body().asHardConstraint()
        }
    }

    fun buildConstraint(
        constraint: UtConstraint
    ) {
        asConstraint { buildConstraintInternal(constraint) }
    }

    private fun buildConstraintInternal(constraint: UtConstraint): UtBoolExpression = with(constraint) {
        when (this) {
            is UtRefEqConstraint -> {
                val lhvVal = buildExpression(lhv)
                val rhvVal = buildExpression(rhv)
                mkEq(lhvVal, rhvVal)
            }
            is UtRefNeqConstraint -> {
                val lhvVal = buildExpression(lhv)
                val rhvVal = buildExpression(rhv)
                mkNot(mkEq(lhvVal, rhvVal))
            }
            is UtRefNotTypeConstraint -> {
                val lhvVal = buildExpression(operand)
                val type = type.toSootType()
                mkNot(engine.typeRegistry.typeConstraint(lhvVal.addr, TypeStorage(type)).isConstraint())
            }
            is UtRefTypeConstraint -> {
                val lhvVal = buildExpression(operand)
                val type = type.toSootType()
                engine.typeRegistry.typeConstraint(lhvVal.addr, TypeStorage(type)).isConstraint()
            }
            is UtAndConstraint -> mkAnd(
                buildConstraintInternal(lhv), buildConstraintInternal(rhv)
            )
            is UtEqConstraint -> {
                val lhvVal = buildExpression(lhv)
                val rhvVal = buildExpression(rhv)
                mkEq(lhvVal, rhvVal)
            }
            is UtGeConstraint -> {
                val lhvVal = buildExpression(lhv) as PrimitiveValue
                val rhvVal = buildExpression(rhv) as PrimitiveValue
                Ge(lhvVal, rhvVal)
            }
            is UtGtConstraint -> {
                val lhvVal = buildExpression(lhv) as PrimitiveValue
                val rhvVal = buildExpression(rhv) as PrimitiveValue
                Gt(lhvVal, rhvVal)
            }
            is UtLeConstraint -> {
                val lhvVal = buildExpression(lhv) as PrimitiveValue
                val rhvVal = buildExpression(rhv) as PrimitiveValue
                Le(lhvVal, rhvVal)
            }
            is UtLtConstraint -> {
                val lhvVal = buildExpression(lhv) as PrimitiveValue
                val rhvVal = buildExpression(rhv) as PrimitiveValue
                Lt(lhvVal, rhvVal)
            }
            is UtNeqConstraint -> {
                val lhvVal = buildExpression(lhv) as PrimitiveValue
                val rhvVal = buildExpression(rhv) as PrimitiveValue
                mkNot(mkEq(lhvVal, rhvVal))
            }
            is UtOrConstraint -> mkOr(
                buildConstraintInternal(lhv), buildConstraintInternal(rhv)
            )
            is UtBoolConstraint -> buildExpression(operand).exprValue as UtBoolExpression
        }
    }

    fun buildExpression(
        variable: UtConstraintVariable
    ): SymbolicValue = with(variable) {
        when (this) {
            is UtConstraintParameter -> when {
                isPrimitive -> {
                    val newName = "post_condition_$name"
                    when (classId) {
                        voidClassId -> voidValue
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
                    val sootType = classId.toSootType().arrayType
                    val addr = UtAddrExpression(mkBVConst("post_condition_${name}", UtIntSort))
                    engine.createArray(addr, sootType, useConcreteType = addr.isThisAddr)
                }
                else -> {
                    val sootType = classId.toSoot().type
                    val addr = UtAddrExpression(mkBVConst("post_condition_${name}", UtIntSort))
                    engine.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
                }
            }
            is UtConstraintBoolConstant -> value.toPrimitiveValue()
            is UtConstraintNumericConstant -> value.primitiveToSymbolic()
            is UtConstraintCharConstant -> value.toPrimitiveValue()
            is UtConstraintArrayAccess -> {
                val arrayInstance = buildExpression(instance)
                val index = buildExpression(index)
                val type = instance.classId.toSootType() as ArrayType
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
            is UtConstraintArrayLengthAccess -> {
                val array = buildExpression(instance)
                engine.memory.findArrayLength(array.addr)
            }
            is UtConstraintFieldAccess -> {
                val type = instance.classId.toSoot().type
                val instanceVal = buildExpression(instance)
                val sootField = fieldId.declaringClass.toSoot().getFieldByName(fieldId.name)
                engine.createFieldOrMock(
                    type,
                    instanceVal.addr,
                    sootField,
                    mockInfoGenerator = null
                )
            }
            is NullUtConstraintVariable -> when {
                classId.isArray -> engine.createArray(nullObjectAddr, classId.toSootType() as ArrayType)
                else -> engine.createObject(
                    nullObjectAddr,
                    classId.toSoot().type,
                    mockInfoGenerator = null,
                    useConcreteType = false
                )
            }
            is UtConstraintAdd -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Add(elhv, erhv)
                )
            }
            is UtConstraintAnd -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    And(elhv, erhv)
                )
            }
            is UtConstraintCmp -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Cmp(elhv, erhv)
                )
            }
            is UtConstraintCmpg -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Cmpg(elhv, erhv)
                )
            }
            is UtConstraintCmpl -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Cmpl(elhv, erhv)
                )
            }
            is UtConstraintDiv ->{
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Div(elhv, erhv)
                )
            }
            is UtConstraintMul -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Mul(elhv, erhv)
                )
            }
            is UtConstraintOr -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Or(elhv, erhv)
                )
            }
            is UtConstraintRem -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Rem(elhv, erhv)
                )
            }
            is UtConstraintShl -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Shl(elhv, erhv)
                )
            }
            is UtConstraintShr ->{
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Shr(elhv, erhv)
                )
            }
            is UtConstraintSub -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Sub(elhv, erhv)
                )
            }
            is UtConstraintUshr -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Ushr(elhv, erhv)
                )
            }
            is UtConstraintXor -> {
                val elhv = buildExpression(lhv) as PrimitiveValue
                val erhv = buildExpression(rhv) as PrimitiveValue
                PrimitiveValue(
                    classId.toSootType(),
                    Xor(elhv, erhv)
                )
            }
            is UtConstraintNot -> {
                val oper = buildExpression(operand) as PrimitiveValue
                PrimitiveValue(oper.type, mkNot(oper.expr as UtBoolExpression))
            }
        }
    }

    private val SymbolicValue.exprValue
        get() = when (this) {
            is ReferenceValue -> addr
            is PrimitiveValue -> expr
        }
}