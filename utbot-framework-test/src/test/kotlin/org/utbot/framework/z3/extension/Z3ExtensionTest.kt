package org.utbot.framework.z3.extension

import org.utbot.engine.z3.Z3Initializer
import com.microsoft.z3.ArrayExpr
import com.microsoft.z3.Context
import com.microsoft.z3.IntNum
import com.microsoft.z3.RatNum
import com.microsoft.z3.SeqExpr
import com.microsoft.z3.Status
import com.microsoft.z3.`=`
import com.microsoft.z3.and
import com.microsoft.z3.declareInt
import com.microsoft.z3.declareIntArray
import com.microsoft.z3.declareList
import com.microsoft.z3.declareReal
import com.microsoft.z3.declareString
import com.microsoft.z3.eval
import com.microsoft.z3.get
import com.microsoft.z3.invoke
import com.microsoft.z3.minus
import com.microsoft.z3.not
import com.microsoft.z3.plus
import com.microsoft.z3.set
import com.microsoft.z3.times
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Z3ExtensionTest : Z3Initializer() {

    @Test
    fun testArrayDefault() {
        Context().use { ctx ->
            val arraySort = ctx.mkArraySort(arrayOf(ctx.mkIntSort(), ctx.mkIntSort()), ctx.mkIntSort())
            val ourArray = ctx.mkConst("a", arraySort) as ArrayExpr

            val solver = ctx.mkSolver()

            solver.add(ctx.mkEq(ctx.mkInt(42), ctx.mkTermArray(ourArray))) //mkTermArray function!
            solver.check()
            val evalArray = solver.model.eval(ourArray)
            assertEquals("((as const (Array Int Int Int)) 42)", evalArray.toString())
        }
    }

    @Test
    fun testInt() {
        Context().use { ctx ->
            val a by ctx.declareInt()
            val b by ctx.declareInt()

            val solver = ctx.mkSolver().apply { add(a * a + b * b `=` 8) }

            assertEquals(Status.SATISFIABLE, solver.check())

            val (aVal, bVal) = solver.model
                .eval(a, b)
                .filterIsInstance<IntNum>()
                .map(IntNum::getInt)
                .also { list -> assertEquals(2, list.size) }

            assertEquals(8, aVal * aVal + bVal * bVal)
        }
    }

    @Test
    fun testReal() {
        Context().use { ctx ->
            val x by ctx.declareReal()

            val solver = ctx.mkSolver().apply {
                add((x * x - x * 4 + 3 `=` 0) and !(x `=` 3))
            }

            assertEquals(Status.SATISFIABLE, solver.check())

            val xVal = (solver.model.eval(x) as RatNum).let {
                it.bigIntNumerator.divide(it.bigIntDenominator).toDouble()
            }

            assertEquals(1.0, xVal, 1E-8)
        }
    }

    @Test
    fun testStrings() {
        Context().use { ctx ->
            val a by ctx.declareString()
            val b by ctx.declareString()
            val c by ctx.declareString()

            val solver = ctx.mkSolver().apply {
                add(a + b `=` "abcd")
                add(b + c `=` "cdef")
                add(!(b `=` ""))
            }

            assertEquals(Status.SATISFIABLE, solver.check())

            val (aVal, bVal, cVal) = solver.model
                .eval(a, b, c)
                .filterIsInstance<SeqExpr>()
                .map(SeqExpr::getString)
                .also { list -> assertEquals(3, list.size) }

            assertEquals("abcd", "$aVal$bVal")
            assertEquals("cdef", "$bVal$cVal")
            assertTrue(bVal.isNotBlank())
        }
    }

    @Test
    fun testArrays() {
        Context().use { ctx ->
            val x by ctx.declareInt()
            val y by ctx.declareInt()
            val a1 by ctx.declareIntArray()

            val solver = ctx.mkSolver().apply {
                add(a1[x] `=` x)
                add(a1.set(x, y) `=` a1)
            }

            assertEquals(Status.SATISFIABLE, solver.check())

            val (xVal, yVal) = solver.model
                .eval(x, y)
                .filterIsInstance<IntNum>()
                .map(IntNum::getInt)
                .also { list -> assertEquals(2, list.size) }

            assertTrue(xVal == yVal)
        }
    }

    @Test
    fun testList() {
        Context().use { ctx ->
            val type = ctx.mkListSort("intList", ctx.intSort)
            val l by ctx.declareList(type)

            val solver = ctx.mkSolver().apply {
                add(!(l `=` type.nil))
                add(type.headDecl(l) `=` 1)
                add(type.tailDecl(l) `=` type.consDecl(type.headDecl(l), type.nil))
            }

            assertEquals(Status.SATISFIABLE, solver.check())

            val lVal = solver.model.eval(l)

            assertEquals("(cons 1 (cons 1 nil))", "$lVal")
        }
    }
}