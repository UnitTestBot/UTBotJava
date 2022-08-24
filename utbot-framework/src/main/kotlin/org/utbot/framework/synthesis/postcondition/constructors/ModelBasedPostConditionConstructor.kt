package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.*
import org.utbot.engine.nullObjectAddr
import org.utbot.engine.pc.*
import org.utbot.engine.symbolic.*
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
import org.utbot.framework.plugin.api.util.*
import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.RefLikeType
import soot.RefType
import soot.Scene
import soot.ShortType
import soot.SootClass
import soot.Type

class ModelBasedPostConditionConstructor(
    val expectedModel: UtModel
) : PostConditionConstructor {
    override fun constructPostCondition(
        traverser: Traverser,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate =
        if (symbolicResult !is SymbolicSuccess) {
            UtFalse.asHardConstraint().asUpdate()
        } else {
            ConstraintBuilder(traverser).run {
                val sootType = expectedModel.classId.toSoot().type
                val addr = UtAddrExpression(mkBVConst("post_condition", UtIntSort))
                val symbValue = traverser.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
                //buildSymbolicValue(symbValue, expectedModel)
                buildSymbolicValue(symbValue, expectedModel)
                constraints + mkEq(symbValue, symbolicResult.value).asHardConstraint()
            }
        }

    override fun constructSoftPostCondition(
        traverser: Traverser,
    ): SymbolicStateUpdate =
        SoftConstraintBuilder(traverser).run {
            val sootType = expectedModel.classId.toSoot().type
            val addr = UtAddrExpression(mkBVConst("post_condition", UtIntSort))
            val symbValue = traverser.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
            buildSymbolicValue(symbValue, expectedModel)
            constraints
        }
}

private class ConstraintBuilder(
    private val traverser: Traverser,
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
                        val fieldSymbolicValue = traverser.createFieldOrMock(
                            type,
                            sv.addr,
                            sootField,
                            mockInfoGenerator = null
                        )
                        traverser.recordInstanceFieldRead(sv.addr, sootField)
                        buildSymbolicValue(fieldSymbolicValue, fieldValue)
                    }
                }
                is UtArrayModel -> {
                    sv as ArrayValue

                    constraints += mkNot(mkEq(nullObjectAddr, sv.addr)).asHardConstraint().asUpdate()

                    for ((index, model) in stores) {
                        val storeSymbolicValue = when (val elementType = sv.type.elementType) {
                            is RefType -> {
                                val objectValue = traverser.createObject(
                                    UtAddrExpression(sv.addr.select(mkInt(index))),
                                    elementType,
                                    useConcreteType = false,
                                    mockInfoGenerator = null
                                )

                                objectValue
                            }
                            is ArrayType -> traverser.createArray(
                                UtAddrExpression(sv.addr.select(mkInt(index))),
                                elementType,
                                useConcreteType = false
                            )
                            else -> PrimitiveValue(elementType, sv.addr.select(mkInt(index)))
                        }

                        buildSymbolicValue(storeSymbolicValue, model)
                    }
                }

                is UtClassRefModel -> {
                    val expected = traverser.createClassRef(this.value.id.toSoot().type)
                    constraints += mkEq(expected.addr, sv.addr).asHardConstraint()
                }

                is UtEnumConstantModel -> {
                    traverser.createEnum(classId.toSoot().type, sv.addr, this.value.ordinal)
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
    private val traverser: Traverser,
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
                        val fieldSymbolicValue = traverser.createFieldOrMock(
                            type,
                            sv.addr,
                            sootField,
                            mockInfoGenerator = null
                        )
                        traverser.recordInstanceFieldRead(sv.addr, sootField)
                        buildSymbolicValue(fieldSymbolicValue, fieldValue)
                    }
                }
                is UtArrayModel -> {
                    sv as ArrayValue

                    constraints += mkNot(mkEq(nullObjectAddr, sv.addr)).asSoftConstraint().asUpdate()

                    for ((index, model) in stores) {
                        val storeSymbolicValue = when (val elementType = sv.type.elementType) {
                            is RefType -> {
                                val objectValue = traverser.createObject(
                                    UtAddrExpression(sv.addr.select(mkInt(index))),
                                    elementType,
                                    useConcreteType = false,
                                    mockInfoGenerator = null
                                )

                                objectValue
                            }
                            is ArrayType -> traverser.createArray(
                                UtAddrExpression(sv.addr.select(mkInt(index))),
                                elementType,
                                useConcreteType = false
                            )
                            else -> PrimitiveValue(elementType, sv.addr.select(mkInt(index)))
                        }

                        buildSymbolicValue(storeSymbolicValue, model)
                    }
                }

                is UtClassRefModel -> {
                    val expected = traverser.createClassRef(this.value.id.toSoot().type)
                    constraints += mkEq(expected.addr, sv.addr).asSoftConstraint()
                }

                is UtEnumConstantModel -> {
                    traverser.createEnum(classId.toSoot().type, sv.addr, this.value.ordinal)
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

internal fun ClassId.toSootType(): Type = when {
    this.isPrimitive -> when (this) {
        booleanClassId -> BooleanType.v()
        byteClassId -> ByteType.v()
        shortClassId -> ShortType.v()
        charClassId -> CharType.v()
        intClassId -> IntType.v()
        longClassId -> LongType.v()
        floatClassId -> FloatType.v()
        doubleClassId -> DoubleType.v()
        else -> error("Unexpected primitive type: $this")
    }
    this.isArray -> elementClassId!!.toSootType().makeArrayType()
    else -> toSoot().type
}