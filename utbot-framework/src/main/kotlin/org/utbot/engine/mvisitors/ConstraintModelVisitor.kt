package org.utbot.engine.mvisitors

import org.utbot.engine.ArrayValue
import org.utbot.engine.MemoryChunkDescriptor
import org.utbot.engine.ObjectValue
import org.utbot.engine.PrimitiveValue
import org.utbot.engine.ReferenceValue
import org.utbot.engine.SymbolicValue
import org.utbot.engine.Traverser
import org.utbot.engine.addr
import org.utbot.engine.findOrdinal
import org.utbot.engine.nullObjectAddr
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.align
import org.utbot.engine.pc.mkEq
import org.utbot.engine.pc.mkNot
import org.utbot.engine.pc.select
import org.utbot.engine.primitiveToLiteral
import org.utbot.engine.primitiveToSymbolic
import org.utbot.engine.toSoot
import org.utbot.engine.toType
import org.utbot.engine.voidValue
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtVoidModel
import soot.ArrayType
import soot.RefType

/**
 * This class builds type and value constraints for equation [symbolicValue] == specific UtModel.
 *
 * Note: may create memory updates in [traverser] while visiting.
 */
class ConstraintModelVisitor(
    private var symbolicValue: SymbolicValue,
    private val traverser: Traverser
) : UtModelVisitor<List<UtBoolExpression>> {

    private inline fun <reified T> withSymbolicValue(
        newSymbolicValue: SymbolicValue,
        block: () -> T
    ): T {
        val old = symbolicValue
        return try {
            symbolicValue = newSymbolicValue
            block()
        } finally {
            symbolicValue = old
        }
    }

    private fun mismatchTypes(): List<UtBoolExpression> =
        emptyList()

    override fun visitArray(model: UtArrayModel): List<UtBoolExpression> =
        when (val value = symbolicValue) {
            is ArrayValue -> {
                val constraints = mutableListOf<UtBoolExpression>()
                constraints += mkNot(mkEq(value.addr, nullObjectAddr))
                val arrayLength = traverser.memory.findArrayLength(value.addr)
                constraints += mkEq(arrayLength.align(), model.length.primitiveToSymbolic())
                val type = model.classId.toType() as ArrayType
                val elementType = model.classId.elementClassId!!.toType()
                val descriptor = MemoryChunkDescriptor(traverser.typeRegistry.arrayChunkId(type), type, elementType)
                val array = traverser.memory.findArray(descriptor)

                repeat(model.length) { storeIndex ->
                    val storeModel = model.stores[storeIndex] ?: model.constModel
                    val selectedExpr = array.select(value.addr, storeIndex.primitiveToLiteral())
                    val storeSymbolicValue = when (elementType) {
                        is RefType -> traverser.createObject(
                            UtAddrExpression(selectedExpr),
                            elementType,
                            useConcreteType = false,
                            mockInfoGenerator = null
                        )
                        is ArrayType -> traverser.createArray(
                            UtAddrExpression(selectedExpr),
                            elementType,
                            useConcreteType = false
                        )
                        else -> PrimitiveValue(elementType, selectedExpr)
                    }
                    constraints += withSymbolicValue(storeSymbolicValue) {
                        storeModel.visit(this)
                    }
                }
                constraints
            }
            else -> mismatchTypes()
        }

    override fun visitAssemble(model: UtAssembleModel): List<UtBoolExpression> {
        // TODO: not supported
        return mismatchTypes()
    }

    override fun visitComposite(model: UtCompositeModel): List<UtBoolExpression> =
        when (val value = symbolicValue) {
            is ObjectValue -> {
                val constraints = mutableListOf<UtBoolExpression>()
                val type = model.classId.toType() as RefType
                val typeStorage = traverser.typeResolver.constructTypeStorage(type, useConcreteType = true)
                constraints += traverser.typeRegistry.typeConstraint(value.addr, typeStorage).isConstraint()
                model.fields.forEach { (field, fieldModel) ->
                    val sootField = field.declaringClass.toSoot().getFieldByName(field.name)
                    val fieldSymbolicValue = traverser.createFieldOrMock(
                        type,
                        value.addr,
                        sootField,
                        mockInfoGenerator = null
                    )
                    traverser.recordInstanceFieldRead(value.addr, sootField)
                    constraints += withSymbolicValue(fieldSymbolicValue) {
                        fieldModel.visit(this)
                    }
                }
                constraints
            }
            else -> mismatchTypes()
        }

    override fun visitNull(model: UtNullModel): List<UtBoolExpression> =
        when (val value = symbolicValue) {
            is ReferenceValue -> listOf(mkEq(value.addr, nullObjectAddr))
            else -> mismatchTypes()
        }

    override fun visitPrimitive(model: UtPrimitiveModel): List<UtBoolExpression> =
        when(val value = symbolicValue) {
            is PrimitiveValue -> listOf(mkEq(value, model.value.primitiveToSymbolic()))
            else -> mismatchTypes()
        }

    override fun visitVoid(model: UtVoidModel): List<UtBoolExpression> =
        when (val value = symbolicValue) {
            is PrimitiveValue -> listOf(mkEq(voidValue, value))
            else -> mismatchTypes()
        }

    override fun visitEnumConstant(model: UtEnumConstantModel): List<UtBoolExpression> =
        when (val value = symbolicValue) {
            is ObjectValue -> {
                val ordinal = traverser.memory.findOrdinal(model.classId.toSoot().type, value.addr)
                listOf(mkEq(ordinal.expr, model.value.ordinal.primitiveToLiteral()))
            }
            else -> mismatchTypes()
        }

    override fun visitClassRef(model: UtClassRefModel): List<UtBoolExpression> =
        when (val value = symbolicValue) {
            is ObjectValue -> {
                val classRef = traverser.createClassRef(model.classId.toSoot().type)
                listOf(mkEq(classRef.addr, value.addr))
            }
            else -> mismatchTypes()
        }
}