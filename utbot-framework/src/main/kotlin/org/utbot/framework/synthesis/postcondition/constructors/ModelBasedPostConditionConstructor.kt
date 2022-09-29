package org.utbot.framework.synthesis.postcondition.constructors

import org.utbot.engine.*
import org.utbot.engine.nullObjectAddr
import org.utbot.engine.pc.*
import org.utbot.engine.selectors.strategies.ModelScoringStrategyBuilder
import org.utbot.engine.selectors.strategies.ScoringStrategyBuilder
import org.utbot.engine.symbolic.*
import org.utbot.framework.plugin.api.*
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
    private val targets: Map<LocalVariable, UtModel>
) : PostConditionConstructor {
    override fun constructPostCondition(
        traverser: Traverser,
        symbolicResult: SymbolicResult?
    ): SymbolicStateUpdate =
        if (symbolicResult !is SymbolicSuccess) {
            UtFalse.asHardConstraint().asUpdate()
        } else {
            ConstraintBuilder(traverser).also {
                for (expectedModel in targets.values) {
                    val sootType = expectedModel.classId.toSoot().type
                    val addr = UtAddrExpression(mkBVConst("post_condition", UtIntSort))
                    val symbValue = traverser.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
                    it.buildSymbolicValue(symbValue, expectedModel)
                    it.constraints += mkEq(symbValue, symbolicResult.value)
                }
            }.asHardConstraints()
        }

    override fun constructSoftPostCondition(
        traverser: Traverser
    ): SymbolicStateUpdate =
        ConstraintBuilder(traverser).also {
            for (expectedModel in targets.values) {
                val sootType = expectedModel.classId.toSoot().type
                val addr = UtAddrExpression(mkBVConst("post_condition", UtIntSort))
                val symbValue = traverser.createObject(addr, sootType, useConcreteType = addr.isThisAddr)
                it.buildSymbolicValue(symbValue, expectedModel)
            }
        }.asSoftConstraints()

    override fun scoringBuilder(): ScoringStrategyBuilder {
        return ModelScoringStrategyBuilder(
            targets
        )
    }
}

private class ConstraintBuilder(
    private val traverser: Traverser
) {
    var constraints = mutableSetOf<UtBoolExpression>()

    fun asHardConstraints() = constraints.asHardConstraint().asUpdate()
    fun asSoftConstraints() = constraints.asSoftConstraint().asUpdate()

    fun buildSymbolicValue(
        sv: SymbolicValue,
        expectedModel: UtModel
    ) {
        with(expectedModel) {
            when (this) {
                is UtPrimitiveModel -> {
                    value.primitiveToSymbolic().apply {
                        constraints += mkEq(this, sv as PrimitiveValue)
                    }
                }
                is UtCompositeModel -> {
                    constraints += mkNot(mkEq(nullObjectAddr, sv.addr))

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

                    constraints += mkNot(mkEq(nullObjectAddr, sv.addr))

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
                    constraints += mkEq(expected.addr, sv.addr)
                }

                is UtEnumConstantModel -> {
                    traverser.createEnum(classId.toSoot().type, sv.addr, this.value.ordinal)
                }

                is UtNullModel -> {
                    constraints += mkEq(nullObjectAddr, sv.addr)
                }

                is UtAssembleModel -> error("Not supported")

                UtVoidModel -> {
                    constraints += mkEq(voidValue, sv)
                }

                is UtConstraintModel -> error("Not supported")

                is UtLambdaModel -> error("Not supported")
            }
        }
    }
}

internal fun ClassId.toSoot(): SootClass = Scene.v().getSootClass(this.name)
