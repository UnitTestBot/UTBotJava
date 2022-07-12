package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.ArrayValue
import org.utbot.engine.PrimitiveValue
import org.utbot.engine.SymbolicResult
import org.utbot.engine.SymbolicSuccess
import org.utbot.engine.SymbolicValue
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.addr
import org.utbot.engine.isThisAddr
import org.utbot.engine.nullObjectAddr
import org.utbot.engine.pc.*
import org.utbot.engine.primitiveToSymbolic
import org.utbot.engine.symbolic.*
import org.utbot.engine.voidValue
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import soot.ArrayType
import soot.BooleanType
import soot.IntType
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.Type

class ModelBasedPostConditionConstructor(
    val expectedModel: UtModel
) : PostConditionConstructor {
    override fun constructPostCondition(
        engine: UtBotSymbolicEngine,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate =
        if (symbolicResult !is SymbolicSuccess) {
            UtFalse.asHardConstraint().asUpdate()
        } else {
            ConstraintBuilder(engine).run {
                val sootType = expectedModel.classId.toSoot().type
                val addr = UtAddrExpression(mkBVConst("post_condition", UtIntSort))
                val symbValue = engine.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
                //buildSymbolicValue(symbValue, expectedModel)
                buildSymbolicValue(symbValue, expectedModel)
                constraints + mkEq(symbValue, symbolicResult.value).asHardConstraint()
            }
        }

    override fun constructSoftPostCondition(
        engine: UtBotSymbolicEngine
    ): SymbolicStateUpdate =
        SoftConstraintBuilder(engine).run {
            val sootType = expectedModel.classId.toSoot().type
            val addr = UtAddrExpression(mkBVConst("post_condition", UtIntSort))
            val symbValue = engine.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
            buildSymbolicValue(symbValue, expectedModel)
            constraints
        }
}

private class ConstraintBuilder(
    private val engine: UtBotSymbolicEngine
) { // TODO : UtModelVisitor<SymbolicValue>() {
    var constraints = SymbolicStateUpdate()

    fun buildSymbolicValue(
        sv: SymbolicValue,
        expectedModel: UtModel
    ) {
        with(expectedModel) {
            when (this) {
                is UtPrimitiveModel -> {
                    value.primitiveToSymbolic().apply {
                        constraints += mkEq(this, sv as PrimitiveValue).asHardConstraint()
                    }
                }
                is UtCompositeModel -> {
                    constraints += mkNot(mkEq(nullObjectAddr, sv.addr)).asHardConstraint().asUpdate()

                    val type = classId.toSoot().type

                    for ((field, fieldValue) in fields) {
                        val sootField = field.declaringClass.toSoot().getFieldByName(field.name)
                        val fieldSymbolicValue = engine.createFieldOrMock(
                            type,
                            sv.addr,
                            sootField,
                            mockInfoGenerator = null
                        )
                        engine.recordInstanceFieldRead(sv.addr, sootField)
                        buildSymbolicValue(fieldSymbolicValue, fieldValue)
                    }
                }
                is UtArrayModel -> {
                    sv as ArrayValue

                    constraints += mkNot(mkEq(nullObjectAddr, sv.addr)).asHardConstraint().asUpdate()

                    for ((index, model) in stores) {
                        val storeSymbolicValue = when (val elementType = sv.type.elementType) {
                            is RefType -> {
                                val objectValue = engine.createObject(
                                    org.utbot.engine.pc.UtAddrExpression(sv.addr.select(mkInt(index))),
                                    elementType,
                                    useConcreteType = false,
                                    mockInfoGenerator = null
                                )

                                objectValue
                            }
                            is ArrayType -> engine.createArray(
                                org.utbot.engine.pc.UtAddrExpression(sv.addr.select(mkInt(index))),
                                elementType,
                                useConcreteType = false
                            )
                            else -> PrimitiveValue(elementType, sv.addr.select(mkInt(index)))
                        }

                        buildSymbolicValue(storeSymbolicValue, model)
                    }
                }

                is UtClassRefModel -> {
                    val expected = engine.createClassRef(this.value.id.toSoot().type)
                    constraints += mkEq(expected.addr, sv.addr).asHardConstraint()
                }

                is UtEnumConstantModel -> {
                    engine.createEnum(classId.toSoot().type, sv.addr, this.value.ordinal)
                }

                is UtNullModel -> {
                    constraints += mkEq(nullObjectAddr, sv.addr).asHardConstraint()
                }

                is UtAssembleModel -> error("Not supported")

                UtVoidModel -> {
                    constraints += mkEq(voidValue, sv).asHardConstraint()
                }
                else -> TODO()
            }
        }
    }
}


private class SoftConstraintBuilder(
    private val engine: UtBotSymbolicEngine
) {
    var constraints = SymbolicStateUpdate()

    fun buildSymbolicValue(
        sv: SymbolicValue,
        expectedModel: UtModel
    ) {
        with(expectedModel) {
            when (this) {
                is UtPrimitiveModel -> {
                    value.primitiveToSymbolic().apply {
                        constraints += mkEq(this, sv as PrimitiveValue).asSoftConstraint()
                    }
                }
                is UtCompositeModel -> {
                    constraints += mkNot(mkEq(nullObjectAddr, sv.addr)).asSoftConstraint().asUpdate()

                    val type = classId.toSoot().type

                    for ((field, fieldValue) in fields) {
                        val sootField = field.declaringClass.toSoot().getFieldByName(field.name)
                        val fieldSymbolicValue = engine.createFieldOrMock(
                            type,
                            sv.addr,
                            sootField,
                            mockInfoGenerator = null
                        )
                        engine.recordInstanceFieldRead(sv.addr, sootField)
                        buildSymbolicValue(fieldSymbolicValue, fieldValue)
                    }
                }
                is UtArrayModel -> {
                    sv as ArrayValue

                    constraints += mkNot(mkEq(nullObjectAddr, sv.addr)).asSoftConstraint().asUpdate()

                    for ((index, model) in stores) {
                        val storeSymbolicValue = when (val elementType = sv.type.elementType) {
                            is RefType -> {
                                val objectValue = engine.createObject(
                                    org.utbot.engine.pc.UtAddrExpression(sv.addr.select(mkInt(index))),
                                    elementType,
                                    useConcreteType = false,
                                    mockInfoGenerator = null
                                )

                                objectValue
                            }
                            is ArrayType -> engine.createArray(
                                org.utbot.engine.pc.UtAddrExpression(sv.addr.select(mkInt(index))),
                                elementType,
                                useConcreteType = false
                            )
                            else -> PrimitiveValue(elementType, sv.addr.select(mkInt(index)))
                        }

                        buildSymbolicValue(storeSymbolicValue, model)
                    }
                }

                is UtClassRefModel -> {
                    val expected = engine.createClassRef(this.value.id.toSoot().type)
                    constraints += mkEq(expected.addr, sv.addr).asSoftConstraint()
                }

                is UtEnumConstantModel -> {
                    engine.createEnum(classId.toSoot().type, sv.addr, this.value.ordinal)
                }

                is UtNullModel -> {
                    constraints += mkEq(nullObjectAddr, sv.addr).asSoftConstraint()
                }

                is UtAssembleModel -> error("Not supported")

                UtVoidModel -> {
                    constraints += mkEq(voidValue, sv).asSoftConstraint()
                }
                else -> TODO()
            }
        }
    }
}

internal fun ClassId.toSoot(): SootClass = Scene.v().getSootClass(this.name)

internal fun ClassId.toSootType(): Type = when (this) {
    booleanClassId -> BooleanType.v()
    intClassId -> IntType.v()
    else -> toSoot().type
}