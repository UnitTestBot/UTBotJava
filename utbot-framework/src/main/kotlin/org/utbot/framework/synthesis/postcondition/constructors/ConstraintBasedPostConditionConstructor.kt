package org.utbot.framework.synthesis.postcondition.constructors

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
        for ((index, model) in models.parameters.withIndex()) {
            val sv = when {
                model.classId.isPrimitive -> engine.environment.state.inputArguments[parameters[index]!!.number]
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
                mkNot(engine.typeRegistry.typeConstraint(lhvVal.addr, TypeStorage(type)).isConstraint())
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
                val array = buildExpression(instance)
                val index = buildExpression(index)
                TODO()
            }
            is UtConstraintArrayLengthAccess -> {
                TODO()
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
        }
    }

    private val SymbolicValue.exprValue
        get() = when (this) {
            is ReferenceValue -> addr
            is PrimitiveValue -> expr
        }
}