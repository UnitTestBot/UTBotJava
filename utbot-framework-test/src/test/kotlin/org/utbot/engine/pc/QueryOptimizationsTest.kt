package org.utbot.engine.pc

import org.utbot.engine.Add
import org.utbot.engine.And
import org.utbot.engine.Cmp
import org.utbot.engine.Cmpg
import org.utbot.engine.Cmpl
import org.utbot.engine.Div
import org.utbot.engine.Eq
import org.utbot.engine.Ge
import org.utbot.engine.Gt
import org.utbot.engine.Le
import org.utbot.engine.Lt
import org.utbot.engine.Mul
import org.utbot.engine.Ne
import org.utbot.engine.Or
import org.utbot.engine.Rem
import org.utbot.engine.Shl
import org.utbot.engine.Shr
import org.utbot.engine.Sub
import org.utbot.engine.Ushr
import org.utbot.engine.Xor
import org.utbot.engine.primitiveToSymbolic
import org.utbot.engine.toBoolValue
import org.utbot.engine.toByteValue
import org.utbot.engine.toDoubleValue
import org.utbot.engine.toFloatValue
import org.utbot.engine.toIntValue
import org.utbot.engine.toLongValue
import org.utbot.engine.toPrimitiveValue
import org.utbot.engine.toShortValue
import org.utbot.framework.UtSettings
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private var expressionSimplificationValue: Boolean = UtSettings.useExpressionSimplification

@BeforeAll
fun beforeAll() {
    UtSettings.useExpressionSimplification = true
}

@AfterAll
fun afterAll() {
    UtSettings.useExpressionSimplification = expressionSimplificationValue
}


class QueryOptimizationsTest {

    private fun BaseQuery.check(vararg exprs: UtBoolExpression, checker: (BaseQuery) -> Unit = {}): BaseQuery =
        this.with(hard = exprs.toList(), soft = emptyList(), assumptions = emptyList()).also {
            checker(it)
        }

    private fun BaseQuery.checkUnsat(vararg exprs: UtBoolExpression): BaseQuery =
        this.check(*exprs) { unsat(it) }

    private val empty: (BaseQuery) -> Unit = { query ->
        assert(query.hard.isEmpty()) { "$query isn't empty" }
    }

    private val notEmpty: (BaseQuery) -> Unit = { query ->
        assert(query.hard.isNotEmpty()) { "$query is empty" }
    }

    private fun size(value: Int): (BaseQuery) -> Unit = { query ->
        assert(query.hard.size == value) { "$query size doesn't match with $value" }
    }

    private fun contains(constraint: UtBoolExpression): (BaseQuery) -> Unit = { query ->
        assert(query.hard.contains(constraint)) { "$query doesn't contain $constraint" }
    }

    private val unsat: (BaseQuery) -> Unit = { query ->
        assert(query.status is UtSolverStatusUNSAT) { "$query isn't unsat" }
    }

    private val unknown: (BaseQuery) -> Unit = { query ->
        assert(query.status is UtSolverStatusUNDEFINED) { "$query status is known" }
    }

    private fun `is`(vararg checkers: (BaseQuery) -> Unit): (BaseQuery) -> Unit = { query ->
        checkers.forEach { it(query) }
    }

    @Test
    fun testConcreteEq() {
        var query: BaseQuery = Query()
        query = query.check(Eq(0.toPrimitiveValue(), 0)) {
            `is`(empty, unknown)(it)
        }
        query.check(Eq(1.toPrimitiveValue(), 0)) {
            `is`(contains(UtFalse), unsat)(it)
        }
    }

    @Test
    fun testSymbolicEq() {
        var query: BaseQuery = Query()
        val p = mkBVConst("p", UtIntSort).toIntValue()
        // p == p === true
        query.check(Eq(p, p)) {
            `is`(empty, unknown)(it)
        }
        // query = Query(p == 0)
        query = query.check(Eq(p, 0)) {
            `is`(notEmpty, unknown)(it)
        }
        // p == 0, p == 1 is unsat
        query.checkUnsat(Eq(p, 1))
        // p == 0, p != 0 is unsat
        query.checkUnsat(Ne(p, 0))
        // p == 0, not (p == 0) is unsat
        query.checkUnsat(NotBoolExpression(Eq(p, 0)))
        // p == 0, p == p === p == 0
        query.check(Eq(p, p)) {
            `is`(size(1), contains(Eq(p, 0)), unknown)(it)
        }
    }

    @Test
    fun testArbitraryExpressionEq() {
        var query: BaseQuery = Query()
        val a = mkBVConst("a", UtIntSort).toIntValue()
        val b = mkBVConst("b", UtIntSort).toIntValue()
        // query = Query(a + b == 0)
        query = query.check(Eq(Add(a, b).toIntValue(), 0)) {
            `is`(size(1), unknown)(it)
        }
        // a + b == 0, a + b == 1 is unsat
        query.checkUnsat(Eq(Add(a, b).toIntValue(), 1))
        val array = mkArrayConst("a", UtIntSort, UtIntSort)
        val first = array.select(mkInt(0)).toIntValue()
        // query = Query(a + b == 0, select(array, 0) == 0)
        query = query.check(Eq(first, 0)) {
            `is`(size(2), unknown)(it)
        }
        // select(array, 0) == 0, select(array, 0) == 1 is unsat
        query.checkUnsat(Eq(first, 1))
    }

    @Test
    fun testEvalIndexOfSelect() {
        var query: BaseQuery = Query()
        val array = mkArrayConst("a", UtIntSort, UtIntSort)
        // query = Query(select(array, 3) == 0)
        query = query.check(Eq(array.select(mkInt(3)).toIntValue(), 0)) {
            `is`(size(1), unknown)(it)
        }
        // select(array, 3) == 0, select(array, 1 + 2) == 2 is unsat
        query.checkUnsat(
            Eq(array.select(Add(1.toPrimitiveValue(), 2.toPrimitiveValue())).toIntValue(), 2)
        )
    }

    @Test
    fun testFpEq() {
        var query: BaseQuery = Query()
        val fp = mkFpConst("a", Double.SIZE_BITS).toDoubleValue()
        // query = Query(a == 0.0)
        query = query.check(Eq(fp, 0.0.toPrimitiveValue())) {
            `is`(size(1), unknown)(it)
        }
        // a == 0.0, a == 1.0 is unsat
        query.checkUnsat(Eq(fp, 1.0.toPrimitiveValue()))
        // a == 0.0, a == 0.0 === a == 0.0
        query.check(Eq(0.0.toPrimitiveValue(), fp)) {
            `is`(size(1), unknown)(it)
        }
    }

    @Test
    fun testOperations() {
        val query: BaseQuery = Query()
        val a = 10.toPrimitiveValue()
        val b = 5.toPrimitiveValue()
        query.check(
            // 10 + 5 == 15
            Eq(Add(a, b).toIntValue(), 15),
            // 10 - 5 == 5
            Eq(Sub(a, b).toIntValue(), 5),
            // 10 * 5 == 50
            Eq(Mul(a, b).toIntValue(), 50),
            // 10 / 5 == 2
            Eq(Div(a, b).toIntValue(), 2),
            // 5 % 10 == 5
            Eq(Rem(b, a).toIntValue(), 5),
            // 10 cmp 5 == 1
            Eq(Cmp(a, b).toIntValue(), 1),
            Eq(Cmpg(a, b).toIntValue(), 1),
            Eq(Cmpl(a, b).toIntValue(), 1),
            // 10 and 5
            Eq(And(a, b).toIntValue(), 10 and 5),
            // 10 or 5
            Eq(Or(a, b).toIntValue(), 10 or 5),
            // 10 xor 5
            Eq(Xor(a, b).toIntValue(), 10 xor 5),
            // 10 << 5
            Eq(Shl(a, b).toIntValue(), 10 shl 5),
            // -1 >> 5 == -1
            Eq(Shr((-1).toPrimitiveValue(), b).toIntValue(), -1 shr 5),
            // -1 >>> 5
            Eq(Ushr((-1).toPrimitiveValue(), b).toIntValue(), -1 ushr 5),
            // 10 << 37 == 10 << 5
            Eq(Shl(a, 37.toPrimitiveValue()).toIntValue(), 10 shl 37),
            // -5 >> 37 == -5 >> 5
            Eq(Shr(mkByte(-5).toIntValue(), 37.toPrimitiveValue()).toIntValue(), -5 shr 37),
            Eq(Ushr((-5).toPrimitiveValue(), 37.toPrimitiveValue()).toIntValue(), -5 ushr 37),
        ) {
            `is`(empty, unknown)(it)
        }
    }

    @Test
    fun testUtEqExpression() {
        var query: BaseQuery = Query()
        // 0 == 0 === true
        query.check(UtEqExpression(mkInt(0), mkInt(0))) {
            `is`(empty, unknown)(it)
        }
        // 1 == 0 === false
        query.checkUnsat(UtEqExpression(mkInt(1), mkInt(0)))
        val array = mkArrayConst("a", UtIntSort, UtIntSort)
        val first = array.select(mkInt(0))
        // query = Query(select(array, 0) == 0)
        query = query.check(UtEqExpression(first, mkInt(0))) {
            `is`(size(1), unknown)(it)
        }
        // select(array, 0) == 0, select(array, 0) == 1 is unsat
        query.checkUnsat(UtEqExpression(first, mkInt(1)))
    }

    @Test
    fun testSplitAndExpression() {
        val query: BaseQuery = Query()
        val a = mkBoolConst("a")
        val b = mkBoolConst("b")
        val c = mkBoolConst("c")
        // a and b and c and true === a, b, c
        query.check(mkAnd(a, b, c, UtTrue)) {
            `is`(size(3), contains(a), contains(b), contains(c), unknown)(it)
        }
    }

    @Test
    fun testAndExpression() {
        val query: BaseQuery = Query()
        val a = mkBoolConst("a")
        val b = mkBoolConst("b")
        val c = mkBoolConst("c")
        // a and b and false === false
        query.check(mkAnd(a, b, UtFalse)) {
            `is`(unsat, contains(UtFalse))(it)
        }
        // a and b and true and c === a and b and c
        query.check(mkAnd(a, b, UtTrue, c)) {
            `is`(size(3), contains(a), contains(b), contains(c), unknown)(it)
        }
        // true and true and true === true
        query.check(mkAnd(UtTrue, UtTrue, UtTrue)) {
            `is`(empty, unknown)(it)
        }
        // a and true === a
        query.check(mkAnd(a, UtTrue)) {
            `is`(size(1), contains(a), unknown)(it)
        }
        // a and b and (true and true) === a and b
        query.check(mkAnd(a, b, (mkAnd(UtTrue, UtTrue)))) {
            `is`(size(2), contains(a), contains(b), unknown)(it)
        }
    }

    @Test
    fun testOrExpression() {
        val query: BaseQuery = Query()
        val a = mkBoolConst("a")
        val b = mkBoolConst("b")
        val c = mkBoolConst("c")
        // a or b or true === a or b
        query.check(mkOr(a, b, UtTrue)) {
            `is`(empty, unknown)(it)
        }
        // a or b or false or c === a or b or c
        query.check(mkOr(a, b, UtFalse, c)) {
            `is`(size(1), contains(mkOr(a, b, c)), unknown)(it)
        }
        // false or false or false === false
        query.checkUnsat(mkOr(UtFalse, UtFalse, UtFalse))
        // a or false === a
        query.check(mkOr(a, UtFalse)) {
            `is`(size(1), contains(a), unknown)(it)
        }
        // a or b or (false or false) === a or b
        query.check(mkOr(a, b, mkOr(UtFalse, UtFalse))) {
            `is`(size(1), contains(mkOr(a, b)))(it)
        }
    }

    @Test
    fun testNotExpression() {
        val query: BaseQuery = Query()
        val a = mkBoolConst("a")
        val b = mkBoolConst("b")
        // not true === false
        query.checkUnsat(NotBoolExpression(UtTrue))
        // not false === true
        query.check(NotBoolExpression(UtFalse)) {
            `is`(empty, unknown)(it)
        }
        // not (a and b) === (not a) or (not b)
        query.check(NotBoolExpression(mkAnd(a, b))) {
            `is`(size(1), contains(mkOr(NotBoolExpression(a), NotBoolExpression(b))), unknown)(it)
        }
        // not (a and true) === (not a)
        query.check(NotBoolExpression(mkAnd(a, UtTrue))) {
            `is`(size(1), contains(NotBoolExpression(a)), unknown)(it)
        }
        // not (a or b) === (not a) and (not b)
        query.check(NotBoolExpression(mkOr(a, b))) {
            `is`(size(2), contains(NotBoolExpression(a)), contains(NotBoolExpression(b)), unknown)(it)
        }
        // not (a or false) === not a
        query.check(NotBoolExpression(mkOr(a, UtFalse))) {
            `is`(size(1), contains(NotBoolExpression(a)), unknown)(it)
        }
        // not (a != b) === a == b
        query.check(NotBoolExpression(Ne(a.toBoolValue(), b.toBoolValue()))) {
            `is`(size(1), contains(Eq(a.toBoolValue(), b.toBoolValue())), unknown)(it)
        }
    }

    @Test
    fun testUtNegExpression() {
        val query: BaseQuery = Query()
        val a = 50
        query.check(
            // neg Byte(50) == Byte(-50)
            Eq(UtNegExpression(a.toByte().toPrimitiveValue()).toByteValue(), (-a).toPrimitiveValue()),
            // neg Short(50) == Short(-50)
            Eq(UtNegExpression(a.toShort().toPrimitiveValue()).toShortValue(), (-a).toPrimitiveValue()),
            // neg 50 == -50
            Eq(UtNegExpression(a.toPrimitiveValue()).toIntValue(), (-a).toPrimitiveValue()),
            // neg Long(50) == Long(-50)
            Eq(UtNegExpression(a.toLong().toPrimitiveValue()).toLongValue(), (-a).toLong().toPrimitiveValue()),
            // neg Float(50) == Float(-50)
            Eq(
                UtNegExpression(a.toFloat().toPrimitiveValue()).toFloatValue(),
                (-a).toFloat().toPrimitiveValue()
            ),
            // neg Double(50) == Double(-50)
            Eq(
                UtNegExpression(a.toDouble().toPrimitiveValue()).toDoubleValue(),
                (-a).toDouble().toPrimitiveValue()
            )
        ) {
            `is`(empty, unknown)(it)
        }
    }

    @Test
    fun testSelectStoreExpression() {
        val query: BaseQuery = Query()
        val constArray = mkArrayWithConst(UtArraySort(UtIntSort, UtIntSort), mkInt(10))
        // select(constArray(1), 10) == 10
        query.check(Eq(constArray.select(mkInt(1)).toIntValue(), 10)) {
            `is`(empty, unknown)(it)
        }
        // select(constArray(1), 10) != 20
        query.checkUnsat(Eq(constArray.select(mkInt(1)).toIntValue(), 20))
        val a = mkBVConst("a", UtIntSort)
        val array = mkArrayConst("array", UtIntSort, UtIntSort)
        val multistore1 = UtArrayMultiStoreExpression(
            array,
            persistentListOf(
                UtStore(mkInt(2), mkInt(10)),
                UtStore(a, mkInt(30)),
                UtStore(mkInt(0), mkInt(10)),
                UtStore(mkInt(0), mkInt(20)),
                UtStore(mkInt(1), mkInt(10))
            )
        )
        val multistore2 = UtArrayMultiStoreExpression(
            array,
            persistentListOf(
                UtStore(mkInt(2), mkInt(10)),
                UtStore(mkInt(0), mkInt(10)),
                UtStore(mkInt(0), mkInt(20)),
                UtStore(mkInt(1), mkInt(10)),
                UtStore(a, mkInt(30))
            )
        )
        query.check(
            // select(store(...., 1, 10), 1) == 10
            Eq(multistore1.select(mkInt(1)).toIntValue(), 10),
            // select(store(store(store(..., 0, 10), 0, 20), 1, 10), 1) == 20
            Eq(multistore1.select(mkInt(0)).toIntValue(), 20),
            // select(store(...., a, 30), a) == 30
            Eq(multistore2.select(a).toIntValue(), 30)
        ) {
            `is`(empty, unknown)(it)
        }
        query.check(
            // select(store(...(store(store(..., 2, 10), a, 30)...), 2) == 10 can't be simplified
            Eq(multistore1.select(mkInt(2)).toIntValue(), 10)
        ) {
            `is`(size(1), unknown)(it)
        }
        query.check(
            // select(store(...(store(..., a, 30))...), a) == 30 can't be simplified
            Eq(multistore1.select(a).toIntValue(), 30)
        ) {
            `is`(size(1), unknown)(it)
        }
    }

    @Test
    fun testPartialArithmeticExpression() {
        val query: BaseQuery = Query()

        val a = mkBVConst("a", UtIntSort).toIntValue()
        val zero = 0.toPrimitiveValue()
        val one = 1.toPrimitiveValue()
        val minusOne = (-1).toPrimitiveValue()
        query.check(
            // a + 0 == a
            Eq(Add(a, zero).toIntValue(), a),
            // a - 0 == a
            Eq(Sub(a, zero).toIntValue(), a),
            // 0 + a == a
            Eq(Add(zero, a).toIntValue(), a),
            // 0 - a == neg a
            Eq(Sub(zero, a).toIntValue(), UtNegExpression(a).toIntValue()),
            // 0 * a == 0
            Eq(Mul(zero, a).toIntValue(), zero),
            Eq(Mul(a, zero).toIntValue(), zero),
            // a * 1 == a
            Eq(Mul(a, one).toIntValue(), a),
            Eq(Mul(one, a).toIntValue(), a),
            // a * (-1) == neg a
            Eq(Mul(a, minusOne).toIntValue(), UtNegExpression(a).toIntValue()),
            Eq(Mul(minusOne, a).toIntValue(), UtNegExpression(a).toIntValue()),
        ) {
            `is`(empty, unknown)(it)
        }
    }

    @Test
    fun testOpenLiteralFromInnerExpr() {
        val query: BaseQuery = Query()

        val a = mkBVConst("a", UtIntSort).toIntValue()
        val five = 5.toPrimitiveValue()
        val ten = 10.toPrimitiveValue()
        val fifteen = 15.toPrimitiveValue()
        val fifty = 50.toPrimitiveValue()
        query.check(
            // ((a + 5) + 10) == a + 15
            Eq(Add(Add(a, five).toIntValue(), ten).toIntValue(), Add(a, fifteen).toIntValue()),
            Eq(Add(Add(five, a).toIntValue(), ten).toIntValue(), Add(a, fifteen).toIntValue()),
            Eq(Add(ten, Add(a, five).toIntValue()).toIntValue(), Add(a, fifteen).toIntValue()),
            Eq(Add(ten, Add(five, a).toIntValue()).toIntValue(), Add(a, fifteen).toIntValue()),
            // ((a * 5) * 10) == a * 50
            Eq(Mul(Mul(a, five).toIntValue(), ten).toIntValue(), Mul(a, fifty).toIntValue()),
            Eq(Mul(Mul(five, a).toIntValue(), ten).toIntValue(), Mul(a, fifty).toIntValue()),
            Eq(Mul(ten, Mul(a, five).toIntValue()).toIntValue(), Mul(a, fifty).toIntValue()),
            Eq(Mul(ten, Mul(five, a).toIntValue()).toIntValue(), Mul(a, fifty).toIntValue()),
            // ((a - 5) + 10) == a + 5
            Eq(Add(Sub(a, five).toIntValue(), ten).toIntValue(), Add(a, five).toIntValue()),
            Eq(Add(ten, Sub(a, five).toIntValue()).toIntValue(), Add(a, five).toIntValue()),
            // ((a + 5) * 10) == a * 10 + 50
            Eq(Mul(Add(a, five).toIntValue(), ten).toIntValue(), Add(Mul(a, ten).toIntValue(), fifty).toIntValue()),
            Eq(Mul(Add(five, a).toIntValue(), ten).toIntValue(), Add(Mul(a, ten).toIntValue(), fifty).toIntValue()),
            Eq(Mul(ten, Add(a, five).toIntValue()).toIntValue(), Add(Mul(a, ten).toIntValue(), fifty).toIntValue()),
            Eq(Mul(ten, Add(five, a).toIntValue()).toIntValue(), Add(Mul(a, ten).toIntValue(), fifty).toIntValue()),
        ) {
            `is`(empty, unknown)(it)
        }
    }

    @Test
    fun testEqSimplificationWithInnerExpr() {
        val query: BaseQuery = Query()

        val a = mkBVConst("a", UtIntSort).toIntValue()
        val five = 5.toPrimitiveValue()
        val ten = 10.toPrimitiveValue()
        val fifteen = 15.toPrimitiveValue()
        // (a + 5) == 10 === a == 5
        query.check(Eq(Add(a, five).toIntValue(), ten)) {
            `is`(contains(Eq(a, five)), unknown)(it)
        }
        // (a - 5) == 10 === a == 15
        query.check(Eq(Sub(a, five).toIntValue(), ten)) {
            `is`(contains(Eq(a, fifteen)), unknown)(it)
        }
        // (a xor 5) == 10 === a == 5 xor 10
        query.check(Eq(Xor(a, five).toIntValue(), ten)) {
            `is`(contains(Eq(a, (10 xor 5).toPrimitiveValue())), unknown)(it)
        }
    }

    @Test
    fun testLtSimplifications() {
        val query: BaseQuery = Query()

        val a = mkBVConst("a", UtIntSort).toIntValue()
        val five = 5.toPrimitiveValue()
        val ten = 10.toPrimitiveValue()
        val fifteen = 15.toPrimitiveValue()
        val nine = 9.toPrimitiveValue()
        val four = 4.toPrimitiveValue()
        // ltQuery = Query(a < 10)
        val ltQuery = query.check(Lt(a, ten)) {
            `is`(size(1), unknown)(it)
        }
        // a < 10, a < 15, a <= 10, a > 5 === a < 10, a > 5
        ltQuery.check(Lt(a, fifteen), Le(a, ten), Gt(a, five)) {
            `is`(contains(Lt(a, ten)), contains(Gt(a, five)), unknown)(it)
        }
        // a < 10, a >= 9 === a == 9
        ltQuery.check(Ge(a, nine)) { `is`(contains(Eq(a, nine)), unknown)(it) }
        // a < 10, a >= 10 is unsat
        ltQuery.checkUnsat(Ge(a, ten))
        // a < 10, a > 10 is unsat
        ltQuery.checkUnsat(Gt(a, ten))
        // a < 10, a == 10 is unsat
        ltQuery.checkUnsat(Eq(a, ten))
        val lessLtQuery = ltQuery.check(Lt(a, five)) { `is`(size(1), unknown)(it) }
        // a < 5, a >= 5 is unsat
        lessLtQuery.checkUnsat(Ge(a, five))
        // a < 5, a >= 4 === a == 4
        lessLtQuery.check(Ge(a, four)) { `is`(contains(Eq(a, four)), unknown)(it) }
        // a < Long.MIN_VALUE is unsat
        query.checkUnsat(Lt(a, Long.MIN_VALUE.primitiveToSymbolic()))
    }

    @Test
    fun testLeSimplifications() {
        val query: BaseQuery = Query()

        val a = mkBVConst("a", UtIntSort).toIntValue()
        val five = 5.toPrimitiveValue()
        val ten = 10.toPrimitiveValue()
        val fifteen = 15.toPrimitiveValue()
        val nine = 9.toPrimitiveValue()
        // geQuery = Query(a <= 10)
        val leQuery = query.check(Le(a, ten)) {
            `is`(size(1), unknown)(it)
        }
        // a <= 10, a < 15, a < 10, a > 5 === a < 10, a > 5
        leQuery.check(Lt(a, fifteen), Lt(a, ten), Gt(a, five)) {
            `is`(contains(Lt(a, ten)), contains(Gt(a, five)), unknown)(it)
        }
        // a <= 10, a > 9  === a == 10
        leQuery.check(Gt(a, nine)) { `is`(contains(Eq(a, ten)), unknown)(it) }
        // a <= 10, a >= 10  === a == 10
        leQuery.check(Ge(a, ten)) { `is`(contains(Eq(a, ten)), unknown)(it) }
        // a <= 10, a > 10 is unsat
        leQuery.checkUnsat(Gt(a, ten))
        // a <= 10, a == 15 is unsat
        leQuery.checkUnsat(Eq(a, fifteen))
        val lessLeQuery = leQuery.check(Le(a, five)) { `is`(size(1), unknown)(it) }
        // a <= 5, a > 5 is unsat
        lessLeQuery.checkUnsat(Gt(a, five))
        // a <= 5, a >= 5 === a == 5
        lessLeQuery.check(Ge(a, five)) { `is`(contains(Eq(a, five)), unknown)(it) }
        // a <= Long.MIN_VALUE is unsat
        query.check(Le(a, Long.MIN_VALUE.primitiveToSymbolic())) { `is`(size(1), unknown)(it) }
    }

    @Test
    fun testGtSimplifications() {
        val query: BaseQuery = Query()

        val a = mkBVConst("a", UtIntSort).toIntValue()
        val five = 5.toPrimitiveValue()
        val ten = 10.toPrimitiveValue()
        val fifteen = 15.toPrimitiveValue()
        val eleven = 11.toPrimitiveValue()
        val sixteen = 16.toPrimitiveValue()
        // geQuery = Query(a > 10)
        val gtQuery = query.check(Gt(a, ten)) {
            `is`(size(1), unknown)(it)
        }
        // a > 10, a > 5, a >= 10, a < 15 === a > 10, a < 15
        gtQuery.check(Gt(a, five), Ge(a, ten), Lt(a, fifteen)) {
            `is`(contains(Gt(a, ten)), contains(Lt(a, fifteen)), unknown)(it)
        }
        // a > 10, a <= 11  === a == 11
        gtQuery.check(Le(a, eleven)) { `is`(contains(Eq(a, eleven)), unknown)(it) }
        // a > 10, a <= 10 is unsat
        gtQuery.checkUnsat(Le(a, ten))
        // a > 10, a < 10 is unsat
        gtQuery.checkUnsat(Lt(a, ten))
        // a > 10, a == 10 is unsat
        gtQuery.checkUnsat(Eq(a, ten))
        val greaterGtQuery = gtQuery.check(Gt(a, fifteen)) { `is`(size(1), unknown)(it) }
        // a > 15, a <= 15 is unsat
        greaterGtQuery.checkUnsat(Le(a, fifteen))
        // a > 15, a <= 16 === a == 16
        greaterGtQuery.check(Le(a, sixteen)) { `is`(contains(Eq(a, sixteen)), unknown)(it) }
        // a > Long.MAX_VALUE is unsat
        query.checkUnsat(Gt(a, Long.MAX_VALUE.primitiveToSymbolic()))
    }

    @Test
    fun testGeSimplifications() {
        val query: BaseQuery = Query()

        val a = mkBVConst("a", UtIntSort).toIntValue()
        val five = 5.toPrimitiveValue()
        val ten = 10.toPrimitiveValue()
        val fifteen = 15.toPrimitiveValue()
        val eleven = 11.toPrimitiveValue()
        // geQuery = Query(a >= 10)
        val geQuery = query.check(Ge(a, ten)) {
            `is`(size(1), unknown)(it)
        }
        // a >= 10, a > 5, a > 10, a < 15 === a > 10, a < 15
        geQuery.check(Gt(a, five), Gt(a, ten), Lt(a, fifteen)) {
            `is`(contains(Gt(a, ten)), contains(Lt(a, fifteen)), unknown)(it)
        }
        // a >= 10, a < 11  === a == 10
        geQuery.check(Lt(a, eleven)) { `is`(contains(Eq(a, ten)), unknown)(it) }
        // a >= 10, a <= 10 === a == 10
        geQuery.check(Le(a, ten)) { `is`(contains(Eq(a, ten)), unknown)(it) }
        // a >= 10, a < 10 is unsat
        geQuery.checkUnsat(Lt(a, ten))
        // a >= 10, a == 5 is unsat
        geQuery.checkUnsat(Eq(a, five))
        val greaterGeQuery = geQuery.check(Ge(a, fifteen)) { `is`(size(1), unknown)(it) }
        // a >= 15, a < 15 is unsat
        greaterGeQuery.checkUnsat(Lt(a, fifteen))
        // a >= 15, a <= 15 === a == 15
        greaterGeQuery.check(Le(a, fifteen)) { `is`(contains(Eq(a, fifteen)), unknown)(it) }
        // a >= Long.MaxVAlue is sat
        query.check(Ge(a, Long.MAX_VALUE.primitiveToSymbolic())) { `is`(size(1), unknown)(it) }
    }
}