package org.utbot.engine.pc

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.PrimitiveValue
import org.utbot.engine.TypeStorage
import org.utbot.engine.UtBinOperator
import org.utbot.engine.UtBoolOperator
import org.utbot.engine.symbolic.SymbolicStateUpdate
import com.microsoft.z3.Expr
import java.util.Objects
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import soot.Type

/**
 * Base class for all expressions that are translated to Z3's expression in [UtSolverStatusSAT.translate] method.
 * @property sort - sort of expression's result
 */
abstract class UtExpression(open val sort: UtSort) {
    protected abstract val hashCode: Int

    abstract fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult
}

val UtExpression.isConcrete: Boolean
    get() = when (this) {
        is UtBvLiteral -> true
        is UtFpLiteral -> true
        is UtSeqLiteral -> true
        is UtBoolLiteral -> true
        is UtAddrExpression -> internal.isConcrete
        else -> false
    }

fun UtExpression.toConcrete(): Any = when (this) {
    is UtBvLiteral -> this.value
    is UtFpLiteral -> this.value
    is UtSeqLiteral -> this.value
    UtTrue -> true
    UtFalse -> false
    is UtAddrExpression -> internal.toConcrete()
    else -> error("Can't get concrete value for $this")
}

abstract class UtArrayExpressionBase(override val sort: UtArraySort) : UtExpression(sort)

sealed class UtInitialArrayExpression(override val sort: UtArraySort) : UtArrayExpressionBase(sort)

data class UtConstArrayExpression(val constValue: UtExpression, override val sort: UtArraySort) :
    UtInitialArrayExpression(sort) {

    override val hashCode = Objects.hash(constValue, sort)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(as const (Array $sort) $constValue)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtConstArrayExpression

        if (constValue != other.constValue) return false
        if (sort != other.sort) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtMkArrayExpression(val name: String, override val sort: UtArraySort) : UtInitialArrayExpression(sort) {
    override val hashCode = Objects.hash(name, sort)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(array $name : $sort)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtMkArrayExpression

        if (name != other.name) return false
        if (sort != other.sort) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtStore(val index: UtExpression, val value: UtExpression) {
    private val hashCode = Objects.hash(index, value)

    override fun toString() = "[$index]=$value"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStore

        if (index != other.index) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * In fact solver has only one store expression. But to avoid StackOverflow we present multiple stores not recursively
 * but like this:
 * `( store
 *    ( store
 *      ( store
 *          array
 *          stores[0].index
 *          stores[0].value
 *       )
 *       stores[1].index
 *       stores[1].value
 *    )
 *    stores[2].index
 *    stores[2].value
 *  )  `
 */
data class UtArrayMultiStoreExpression(
    val initial: UtExpression,
    val stores: PersistentList<UtStore>
) : UtArrayExpressionBase(initial.sort as UtArraySort) {
    init {
        val initialSort = initial.sort as UtArraySort
        require(
            stores.all {
                it.value.sort == initialSort.itemSort &&
                it.index.sort == initialSort.indexSort
            }
        ) {
            "Unequal sorts found during the UtArrayMultiStoreExpression initialization"
        }
    }

    override val hashCode = Objects.hash(initial, stores)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(store $initial ${stores.joinToString()})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayMultiStoreExpression

        if (initial != other.initial) return false
        if (stores != other.stores) return false

        return true
    }

    override fun hashCode() = hashCode

    companion object {
        operator fun invoke(
            array: UtExpression,
            index: UtExpression,
            elem: UtExpression
        ): UtArrayMultiStoreExpression {
            val store = UtStore(index, elem)

            require(array.sort is UtArraySort) {
                "An attempt to store $elem to the expression with ${array.sort} sort"
            }
            val arraySort = array.sort as UtArraySort
            require(arraySort.indexSort == index.sort) {
                "Given index has sort ${index.sort}, but ${arraySort.indexSort} is required"
            }
            require(arraySort.itemSort == elem.sort ||
                    workaround(WorkaroundReason.IGNORE_SORT_INEQUALITY) {
                        equalSorts(arraySort.itemSort, elem.sort)
                    }) {
                "Given elem has sort ${elem.sort}, but ${arraySort.itemSort} is required"
            }

            return when (array) {
                is UtInitialArrayExpression, is UtArraySelectExpression -> UtArrayMultiStoreExpression(
                    array,
                    persistentListOf(store)
                )
                is UtArrayMultiStoreExpression -> UtArrayMultiStoreExpression(array.initial, array.stores.add(store))
                else -> error("Unexpected store into ${array::class}")
            }
        }
    }
}

data class UtArraySelectExpression(val arrayExpression: UtExpression, val index: UtExpression) :
    UtExpression(arrayExpression.toSelectSort()) {

    init {
        require(arrayExpression.sort is UtArraySort)
        val arraySort = arrayExpression.sort as UtArraySort
        require(
            arraySort.indexSort == index.sort || workaround(WorkaroundReason.IGNORE_SORT_INEQUALITY) {
                equalSorts(arraySort.indexSort, index.sort)
            }
        )
    }

    override val hashCode = Objects.hash(arrayExpression, index)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(select $arrayExpression $index)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArraySelectExpression

        if (arrayExpression != other.arrayExpression) return false
        if (index != other.index) return false

        return true
    }

    override fun hashCode() = hashCode

    // TODO error messages
    companion object {
        private fun UtExpression.toSelectSort() = when (this) {
            is UtArrayExpressionBase -> sort.itemSort
            is UtArraySelectExpression -> (sort as? UtArraySort)?.itemSort ?: error("Wrong sort $sort")
            else -> error("Wrong selectSort call for $sort")
        }
    }
}

/**
 * Uses in [org.utbot.engine.Traverser.classCastExceptionCheck].
 * Returns the most nested index in the [UtArraySelectExpression].
 *
 * I.e. for (select a i) it returns i, for (select (select (select a i) j) k) it still returns i
 * and for (select (store (select a i) ..) j) it still returns i.
 */
fun findTheMostNestedAddr(expr: UtArraySelectExpression): UtAddrExpression {
    var nestedExpr: UtExpression = expr
    var nestedAddr = expr.index

    while (true) {
        nestedExpr = when (nestedExpr) {
            is UtArrayInsert -> nestedExpr.arrayExpression
            is UtArrayInsertRange -> nestedExpr.arrayExpression
            is UtArrayRemove -> nestedExpr.arrayExpression
            is UtArrayRemoveRange -> nestedExpr.arrayExpression
            is UtArraySetRange -> nestedExpr.arrayExpression
            is UtArrayShiftIndexes -> nestedExpr.arrayExpression
            is UtArrayApplyForAll -> nestedExpr.arrayExpression
            is UtArrayMultiStoreExpression -> nestedExpr.initial
            is UtArraySelectExpression -> {
                nestedAddr = nestedExpr.index
                nestedExpr.arrayExpression
            }
            is UtConstArrayExpression, is UtMkArrayExpression -> break
            else -> error("Unexpected type of expression: $nestedExpr")
        }
    }
    while (true) {
        nestedAddr = when (nestedAddr) {
            is UtAddrExpression -> nestedAddr.internal
            is UtArraySelectExpression -> nestedAddr.index
            else -> break
        }
    }
    return UtAddrExpression(nestedAddr)
}

//INTEGRAL only, char is not presented here
abstract class UtBvExpression(sort: UtBvSort) : UtExpression(sort) {
    val size = sort.size
}

data class UtBvLiteral(val value: Number, override val sort: UtBvSort) : UtBvExpression(sort) {
    override val hashCode = Objects.hash(value, size)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "$sort $value"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtBvLiteral

        if (value != other.value) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtBvConst(val name: String, override val sort: UtBvSort) : UtBvExpression(sort) {
    override val hashCode = Objects.hash(name, size)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(BV$sort $name)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtBvConst

        if (name != other.name) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * A class representing type for an object
 * @param addr Object's symbolic address.
 * @param typeStorage Object's type holder.
 * @param numberOfTypes Number of types in the whole program.
 * @see org.utbot.engine.TypeStorage
 */
class UtIsExpression(
    val addr: UtAddrExpression,
    val typeStorage: TypeStorage,
    val numberOfTypes: Int
) : UtBoolExpression() {
    val type: Type get() = typeStorage.leastCommonType

    override val hashCode = Objects.hash(addr, typeStorage, numberOfTypes)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString(): String {
        val possibleTypes = if (typeStorage.possibleConcreteTypes.size == 1) {
            ""
        } else {
            ". Possible types number: ${typeStorage.possibleConcreteTypes.size}"
        }

        return "(is $addr ${typeStorage.leastCommonType}$possibleTypes)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtIsExpression

        if (addr != other.addr) return false
        if (typeStorage != other.typeStorage) return false
        if (numberOfTypes != other.numberOfTypes) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * [UtBoolExpression] that represents that an object with address [addr] is parameterized by [types]
 */
class UtGenericExpression(
    val addr: UtAddrExpression,
    val types: List<TypeStorage>,
    val numberOfTypes: Int
) : UtBoolExpression() {
    override val hashCode = Objects.hash(addr, types, numberOfTypes)
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString(): String {
        val postfix = types.joinToString(",", "<", ">")
        return "(generic $addr $postfix)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtGenericExpression

        if (addr != other.addr) return false
        if (types != other.types) return false
        if (numberOfTypes != other.numberOfTypes) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * [UtBoolExpression] class that represents that type parameters of an object with address [firstAddr] are equal to
 * type parameters of an object with address [secondAddr], corresponding to [indexMapping].
 *
 * For instance, if the second object is parametrized by <Int, Double> and indexMapping is (0 to 1, 1 to 0) then this
 * expression is true, when the first object is parametrized by <Double, Int>
 */
class UtEqGenericTypeParametersExpression(
    val firstAddr: UtAddrExpression,
    val secondAddr: UtAddrExpression,
    val indexMapping: Map<Int, Int>
) : UtBoolExpression() {
    override val hashCode = Objects.hash(firstAddr, secondAddr, indexMapping)
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString(): String {
        return "(generic-eq $firstAddr $secondAddr by <${indexMapping.toList()}>)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtEqGenericTypeParametersExpression

        if (firstAddr != other.firstAddr) return false
        if (secondAddr != other.secondAddr) return false
        if (indexMapping != other.indexMapping) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * [UtBoolExpression] that represents that an object with address [addr] has the same type as the type parameter
 * with index [parameterTypeIndex] of an object with address [baseAddr]
 *
 * For instance, if the base object is parametrized by <Int, Double> and parameterTypeIndex = 1, then this expression is
 * true when the object with address addr is Double
 */
class UtIsGenericTypeExpression(
    val addr: UtAddrExpression,
    val baseAddr: UtAddrExpression,
    val parameterTypeIndex: Int
) : UtBoolExpression() {
    override val hashCode = Objects.hash(addr, baseAddr, parameterTypeIndex)
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString(): String {
        return "(generic-is $addr $baseAddr<\$$parameterTypeIndex>)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtIsGenericTypeExpression

        if (addr != other.addr) return false
        if (baseAddr != other.baseAddr) return false
        if (parameterTypeIndex != other.parameterTypeIndex) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * This class represents a result of the `instanceof` instruction.
 * DO NOT MIX IT UP WITH [UtIsExpression]. This one should be used in the result of the instanceof instruction ONLY.
 * It does NOT represent types for objects.
 *
 * [UtInstanceOfExpression.constraint] predicate that should be fulfilled in the branch where `(a instanceof type)` is true.
 * @param symbolicStateUpdate symbolic state update for the returning true branch of the instanceof expression. Used to
 * update type of array in the code like this: (Object instanceof int[]). The object should be updated to int[]
 * in the memory.
 *
 * @see UtIsExpression
 */
class UtInstanceOfExpression(
    val symbolicStateUpdate: SymbolicStateUpdate = SymbolicStateUpdate()
) : UtBoolExpression() {
    val constraint: UtBoolExpression get() = mkAnd(symbolicStateUpdate.hardConstraints.constraints.toList())
    override val hashCode = symbolicStateUpdate.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "$constraint"
}

//ADDRESS
@Suppress("DataClassPrivateConstructor")
data class UtAddrExpression private constructor(val internal: UtExpression) : UtExpression(UtAddrSort) {
    companion object {
        operator fun invoke(internal: UtExpression) =
            if (internal is UtAddrExpression) internal
            else UtAddrExpression(internal)

        operator fun invoke(addr: Int) = UtAddrExpression(mkInt(addr))
    }

    override val hashCode = internal.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "addr: $internal"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtAddrExpression

        if (internal != other.internal) return false

        return true
    }

    override fun hashCode() = hashCode
}


//FLOATING POINT
sealed class UtFpExpression(open val size: Int) : UtExpression(
    when (size) {
        Float.SIZE_BITS -> UtFp32Sort
        Double.SIZE_BITS -> UtFp64Sort
        else -> error("Unknown size $size")
    }
)

data class UtFpLiteral(val value: Number, override val size: Int) : UtFpExpression(size) {
    override val hashCode = Objects.hash(value, size)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "$sort $value"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtFpLiteral

        if (value != other.value) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtFpConst(val name: String, override val size: Int) : UtFpExpression(size) {
    override val hashCode = Objects.hash(name, size)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(fp$size $name)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtFpConst

        if (name != other.name) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtOpExpression(
    val operator: UtBinOperator,
    val left: PrimitiveValue,
    val right: PrimitiveValue,
    val resultSort: UtSort
) : UtExpression(resultSort) {
    override val hashCode = Objects.hash(operator, left, right, resultSort)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "($operator $left $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtOpExpression

        if (operator != other.operator) return false
        if (left != other.left) return false
        if (right != other.right) return false
        if (resultSort != other.resultSort) return false

        return true
    }

    override fun hashCode() = hashCode
}

abstract class UtBoolExpression : UtExpression(UtBoolSort)

sealed class UtBoolLiteral(val value: Boolean) : UtBoolExpression()

object UtTrue : UtBoolLiteral(true) {
    override val hashCode = true.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "true"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return true
    }

    override fun hashCode() = hashCode
}

object UtFalse : UtBoolLiteral(false) {
    override val hashCode = false.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "false"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtEqExpression(val left: UtExpression, val right: UtExpression) : UtBoolExpression() {
    override val hashCode = Objects.hash(left, right)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(eq $left $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtEqExpression

        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtBoolConst(val name: String) : UtBoolExpression() {
    override val hashCode = name.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(bool $name)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtBoolConst

        if (name != other.name) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class NotBoolExpression(val expr: UtBoolExpression) : UtBoolExpression() {
    override val hashCode = expr.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(not $expr)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotBoolExpression

        if (expr != other.expr) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtOrBoolExpression(val exprs: List<UtBoolExpression>) : UtBoolExpression() {
    override val hashCode = exprs.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(or ${exprs.joinToString()})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtOrBoolExpression

        if (exprs != other.exprs) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtAndBoolExpression(val exprs: List<UtBoolExpression>) : UtBoolExpression() {
    override val hashCode = exprs.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(and ${exprs.joinToString()})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtAndBoolExpression

        if (exprs != other.exprs) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtAddNoOverflowExpression(
    val left: UtExpression,
    val right: UtExpression,
) : UtBoolExpression() {
    override val hashCode = Objects.hash(left, right)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(addNoOverflow $left $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtAddNoOverflowExpression

        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtSubNoOverflowExpression(
    val left: UtExpression,
    val right: UtExpression,
) : UtBoolExpression() {
    override val hashCode = Objects.hash(left, right)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(subNoOverflow $left $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtSubNoOverflowExpression

        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtNegExpression(val variable: PrimitiveValue) : UtExpression(alignSort(variable.type.toSort())) {
    override val hashCode = variable.hashCode

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(neg ${variable.expr})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtNegExpression

        if (variable != other.variable) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * Implements Unary Numeric Promotion.
 * Converts byte, short and char sort to int sort.
 */
fun alignSort(sort: UtSort): UtSort = when {
    sort is UtBvSort && sort.size < Int.SIZE_BITS -> UtIntSort
    else -> sort
}

data class UtCastExpression(val variable: PrimitiveValue, val type: Type) : UtExpression(type.toSort()) {
    override val hashCode = Objects.hash(variable, type)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtCastExpression

        if (variable != other.variable) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtBoolOpExpression(
    val operator: UtBoolOperator,
    val left: PrimitiveValue,
    val right: PrimitiveValue
) : UtBoolExpression() {
    override val hashCode = Objects.hash(operator, left, right)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "($operator $left $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtBoolOpExpression

        if (operator != other.operator) return false
        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtIteExpression(
    val condition: UtBoolExpression,
    val thenExpr: UtExpression,
    val elseExpr: UtExpression
) : UtExpression(thenExpr.sort) {
    override val hashCode = Objects.hash(condition, thenExpr, elseExpr)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(ite $condition $thenExpr $elseExpr)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtIteExpression

        if (condition != other.condition) return false
        if (thenExpr != other.thenExpr) return false
        if (elseExpr != other.elseExpr) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtMkTermArrayExpression(val array: UtArrayExpressionBase, val defaultValue: UtExpression? = null) :
    UtBoolExpression() {
    override val hashCode = Objects.hash(array, defaultValue)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(mkTermArray $array)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtMkTermArrayExpression

        if (array != other.array) return false
        if (defaultValue != other.defaultValue) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class Z3Variable(val type: Type, val expr: Expr) {
    private val hashCode = Objects.hash(type, expr)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Z3Variable

        if (type != other.type) return false
        if (expr != other.expr) return false

        return true
    }

    override fun hashCode() = hashCode
}

fun Expr.z3Variable(type: Type) = Z3Variable(type, this)

fun UtExpression.isInteger() = this.sort is UtBvSort