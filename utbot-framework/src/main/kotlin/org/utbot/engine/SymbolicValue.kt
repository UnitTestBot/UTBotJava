package org.utbot.engine

import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.isConcrete
import org.utbot.engine.pc.mkBool
import org.utbot.engine.pc.mkByte
import org.utbot.engine.pc.mkChar
import org.utbot.engine.pc.mkDouble
import org.utbot.engine.pc.mkFloat
import org.utbot.engine.pc.mkInt
import org.utbot.engine.pc.mkLong
import org.utbot.engine.pc.mkShort
import org.utbot.engine.pc.toConcrete
import java.util.Objects
import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.RefType
import soot.ShortType
import soot.Type
import soot.VoidType

/**
 * Base class for all symbolic memory cells: primitive, reference, arrays
 */
sealed class SymbolicValue {
    abstract val concrete: Concrete?
    abstract val typeStorage: TypeStorage
    abstract val type: Type
    abstract val hashCode: Int
    val possibleConcreteTypes get() = typeStorage.possibleConcreteTypes
}

/**
 * Wrapper that contains concrete value in given memory cells. Could be used for optimizations or wrappers (when you
 * do not do symbolic execution honestly but invoke tweaked behavior).
 */
data class Concrete(val value: Any?)

/**
 * Memory cell that contains primitive value as the result
 */
data class PrimitiveValue(
    override val typeStorage: TypeStorage,
    val expr: UtExpression,
    override val concrete: Concrete? = null
) : SymbolicValue() {
    constructor(type: Type, expr: UtExpression) : this(TypeStorage(type), expr)

    override val type get() = typeStorage.leastCommonType

    override val hashCode = Objects.hash(typeStorage, expr, concrete)

    override fun toString() = "($type $expr)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PrimitiveValue

        if (typeStorage != other.typeStorage) return false
        if (expr != other.expr) return false
        if (concrete != other.concrete) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * Memory cell that can contain any reference value: array or object
 */
sealed class ReferenceValue(open val addr: UtAddrExpression) : SymbolicValue()


/**
 * Memory cell contains ordinal objects (not arrays).
 *
 * Note: if you create an object, be sure you add constraints for its type using [TypeConstraint],
 * otherwise it is possible for an object to have inappropriate or incorrect typeId and dimensionNum.
 *
 * @see TypeRegistry.typeConstraint
 * @see Traverser.createObject
 */
data class ObjectValue(
    override val typeStorage: TypeStorage,
    override val addr: UtAddrExpression,
    override val concrete: Concrete? = null
) : ReferenceValue(addr) {
    override val type: RefType get() = typeStorage.leastCommonType as RefType

    override val hashCode = Objects.hash(typeStorage, addr, concrete)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ObjectValue

        if (typeStorage != other.typeStorage) return false
        if (addr != other.addr) return false
        if (concrete != other.concrete) return false

        return true
    }

    override fun hashCode() = hashCode

    override fun toString() = "ObjectValue(typeStorage=$typeStorage, addr=$addr${concretePart()})"

    private fun concretePart() = concrete?.let { ", concrete=$concrete" } ?: ""
}


/**
 * Memory cell contains java arrays.
 *
 * Note: if you create an array, be sure you add constraints for its type using [TypeConstraint],
 * otherwise it is possible for an object to have inappropriate or incorrect typeId and dimensionNum.
 *
 * @see TypeRegistry.typeConstraint
 * @see Traverser.createObject
 */
data class ArrayValue(
    override val typeStorage: TypeStorage,
    override val addr: UtAddrExpression,
    override val concrete: Concrete? = null
) : ReferenceValue(addr) {
    override val type get() = typeStorage.leastCommonType as ArrayType

    override val hashCode = Objects.hash(typeStorage, addr, concrete)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayValue

        if (typeStorage != other.typeStorage) return false
        if (addr != other.addr) return false
        if (concrete != other.concrete) return false

        return true
    }

    override fun hashCode() = hashCode
}

val SymbolicValue.asPrimitiveValueOrError
    get() = (this as? PrimitiveValue)?.expr ?: error("${this::class} is not a primitive")

val SymbolicValue.asWrapperOrNull
    get() = concrete?.value as? WrapperInterface

val SymbolicValue.addr
    get() = when (this) {
        is ReferenceValue -> addr
        is PrimitiveValue -> error("PrimitiveValue $this doesn't have an address")
    }

val SymbolicValue.isConcrete: Boolean
    get() = when (this) {
        is PrimitiveValue -> this.expr.isConcrete
        is ArrayValue, is ObjectValue -> false
    }

fun SymbolicValue.toConcrete(): Any = when (this) {
        is PrimitiveValue -> this.expr.toConcrete()
        is ArrayValue, is ObjectValue -> error("Can't get concrete value for $this")
    }

// TODO: one more constructor?
fun objectValue(type: RefType, addr: UtAddrExpression, implementation: WrapperInterface) =
    ObjectValue(TypeStorage(type), addr, Concrete(implementation))

val voidValue
    get() = PrimitiveValue(VoidType.v(), nullObjectAddr)

fun UtExpression.toPrimitiveValue(type: Type) = PrimitiveValue(type, this)
fun UtExpression.toByteValue() = this.toPrimitiveValue(ByteType.v())
fun UtExpression.toShortValue() = this.toPrimitiveValue(ShortType.v())
fun UtExpression.toCharValue() = this.toPrimitiveValue(CharType.v())
fun UtExpression.toLongValue() = this.toPrimitiveValue(LongType.v())
fun UtExpression.toIntValue() = this.toPrimitiveValue(IntType.v())
fun UtExpression.toFloatValue() = this.toPrimitiveValue(FloatType.v())
fun UtExpression.toDoubleValue() = this.toPrimitiveValue(DoubleType.v())
fun UtExpression.toBoolValue() = this.toPrimitiveValue(BooleanType.v())

fun Byte.toPrimitiveValue() = mkByte(this).toByteValue()
fun Short.toPrimitiveValue() = mkShort(this).toShortValue()
fun Char.toPrimitiveValue() = mkChar(this).toCharValue()
fun Int.toPrimitiveValue() = mkInt(this).toIntValue()
fun Long.toPrimitiveValue() = mkLong(this).toLongValue()
fun Float.toPrimitiveValue() = mkFloat(this).toFloatValue()
fun Double.toPrimitiveValue() = mkDouble(this).toDoubleValue()
fun Boolean.toPrimitiveValue() = mkBool(this).toBoolValue()

