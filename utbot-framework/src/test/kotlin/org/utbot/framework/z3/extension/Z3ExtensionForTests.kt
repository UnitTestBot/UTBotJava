@file:Suppress("PackageDirectoryMismatch", "unused", "NonAsciiCharacters")

package com.microsoft.z3

import kotlin.reflect.KProperty


operator fun ArithExpr.plus(other: ArithExpr): ArithExpr = context.mkAdd(this, other)
operator fun ArithExpr.plus(other: Int): ArithExpr = this + context.mkInt(other)

operator fun ArithExpr.minus(other: ArithExpr): ArithExpr = context.mkSub(this, other)
operator fun ArithExpr.minus(other: Int): ArithExpr = this - context.mkInt(other)

operator fun ArithExpr.times(other: ArithExpr): ArithExpr = context.mkMul(this, other)
operator fun ArithExpr.times(other: Int): ArithExpr = this * context.mkInt(other)

infix fun Expr.`=`(other: Int): BoolExpr = this eq context.mkInt(other)
infix fun Expr.`=`(other: Expr): BoolExpr = this eq other
infix fun Expr.eq(other: Expr): BoolExpr = context.mkEq(this, other)
infix fun Expr.`!=`(other: Expr): BoolExpr = context.mkNot(this `=` other)
operator fun BoolExpr.not(): BoolExpr = context.mkNot(this)

infix fun BoolExpr.`⇒`(other: BoolExpr): BoolExpr = this implies other
infix fun BoolExpr.`⇒`(other: Boolean): BoolExpr = this implies other
infix fun BoolExpr.implies(other: Boolean): BoolExpr = this implies context.mkBool(other)
infix fun BoolExpr.implies(other: BoolExpr): BoolExpr = context.mkImplies(this, other)
infix fun BoolExpr.and(other: BoolExpr): BoolExpr = context.mkAnd(this, other)
infix fun BoolExpr.or(other: BoolExpr): BoolExpr = context.mkOr(this, other)

infix fun ArithExpr.gt(other: ArithExpr): BoolExpr = context.mkGt(this, other)
infix fun ArithExpr.gt(other: Int): BoolExpr = context.mkGt(this, context.mkInt(other))

infix fun ArithExpr.`=`(other: Int): BoolExpr = context.mkEq(this, context.mkInt(other))

operator fun ArrayExpr.get(index: IntExpr): Expr = context.mkSelect(this, index)
operator fun ArrayExpr.get(index: Int): Expr = this[context.mkInt(index)]
fun ArrayExpr.set(index: IntExpr, value: Expr): ArrayExpr = context.mkStore(this, index, value)
fun ArrayExpr.set(index: Int, value: Expr): ArrayExpr = set(context.mkInt(index), value)

operator fun SeqExpr.plus(other: SeqExpr): SeqExpr = context.mkConcat(this, other)
operator fun SeqExpr.plus(other: String): SeqExpr = context.mkConcat(context.mkString(other))

infix fun SeqExpr.`=`(other: String): BoolExpr = context.mkEq(this, context.mkString(other))

class Const<T>(private val ctx: Context, val produce: (Context, name: String) -> T) {
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): T = produce(ctx, property.name)
}

fun Context.declareInt() = Const(this) { ctx, name -> ctx.mkIntConst(name) }
fun Context.declareBool() = Const(this) { ctx, name -> ctx.mkBoolConst(name) }
fun Context.declareReal() = Const(this) { ctx, name -> ctx.mkRealConst(name) }
fun Context.declareString() = Const(this) { ctx, name -> ctx.mkConst(name, stringSort) as SeqExpr }
fun Context.declareList(sort: ListSort) = Const(this) { ctx, name -> ctx.mkConst(name, sort) }
fun Context.declareIntArray() = Const(this) { ctx, name -> ctx.mkArrayConst(name, intSort, intSort) }


operator fun FuncDecl.invoke(vararg expr: Expr): Expr = context.mkApp(this, *expr)

fun Model.eval(expr: Expr): Expr = this.eval(expr, true)
fun Model.eval(vararg exprs: Expr): List<Expr> = exprs.map { this.eval(it, true) }