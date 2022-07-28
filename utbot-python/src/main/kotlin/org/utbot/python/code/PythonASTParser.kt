package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.*
import io.github.danielnaczo.python3parser.model.expr.datastructures.Dict
import io.github.danielnaczo.python3parser.model.expr.datastructures.Set
import io.github.danielnaczo.python3parser.model.expr.datastructures.ListExpr
import io.github.danielnaczo.python3parser.model.expr.datastructures.Tuple
import io.github.danielnaczo.python3parser.model.expr.operators.Operator
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.BinOp
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AugAssign
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Arguments
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Keyword
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.Attribute
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.subscripts.Index
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.subscripts.Subscript
import io.github.danielnaczo.python3parser.model.expr.operators.unaryops.UnaryOp
import org.apache.commons.lang3.math.NumberUtils
import java.math.BigInteger

sealed class Result<T>
class Match<T>(val value: T): Result<T>()
class Error<T> : Result<T>()

open class Parser<A, B, N> (
    val go: (N, A) -> Result<B>
)

fun <A, B, N> parse(pat: Parser<A, B, N>, onError: B, node: N, x: A): B {
    return when (val result = pat.go(node, x)) {
        is Match -> result.value
        is Error -> onError
    }
}

inline fun <A, B, reified N, M> refl(pat: Parser<A, B, N>): Parser<A, B, M> =
    Parser { node, x ->
        when (node) {
            is N -> pat.go(node, x)
            else -> Error()
        }
    }

fun <A, N> drop(): Parser<A, A, N> =
    Parser { _, x ->  Match(x) }

fun <A, N> apply(): Parser<(N) -> A, A, N> =
    Parser { node, x -> Match(x(node)) }

fun <A, B> name(fid: Parser<A, B, String>): Parser<A, B, Name> =
    Parser { node, x ->
        fid.go(node.id.name, x)
    }

fun <A> int(): Parser<A, A, String> =
    Parser { node, x ->
        when (NumberUtils.createNumber(node)) {
            is Int -> Match(x)
            is Long -> Match(x)
            is BigInteger -> Match(x)
            else -> Error()
        }
    }

fun <A, B> num(fnum: Parser<A, B, String>): Parser<A, B, Num> = Parser { node, x -> fnum.go(node.n, x) }
fun <A, B> str(fstr: Parser<A, B, String>): Parser<A, B, Str> = Parser { node, x -> fstr.go(node.s, x) }
fun <A> true_(): Parser<A, A, True> = Parser { _, x -> Match(x) }
fun <A> false_(): Parser<A, A, False> = Parser { _, x -> Match(x) }
fun <A> none(): Parser<A, A, None> = Parser { _, x -> Match(x) }

fun <A, N> equal(value: N): Parser<A, A, N> =
    Parser { node, x ->
        if (node != value)
            Error()
        else
            Match(x)
    }

fun <A, B, N> go(y: Parser<A, B, N>, node: N, x: Result<A>) =
    when (x) {
        is Match -> y.go(node, x.value)
        is Error -> Error()
    }

fun <A> toMatch(x: Result<List<A>>): Match<List<A>> =
    when (x) {
        is Error -> Match(emptyList())
        is Match -> x
    }

fun <A, B, N> multiGo(y: Parser<A, B, N>, node: N, x: Result<List<A>>): Result<List<B>> {
    val res = mutableListOf<B>()
    for (z in (toMatch(x)).value) {
        when (val w = y.go(node, z)) {
            is Error -> Unit
            is Match -> res.add(w.value)
        }
    }
    return Match(res)
}

fun <A, B, C> assign(
    ftargets: Parser<A, B, List<Expression>>,
    fvalue: Parser<B, C, Expression>
): Parser<A, C, Assign> =
    Parser { node, x ->
        if (!node.value.isPresent)
            return@Parser Error()
        val x1 = ftargets.go(node.targets, x)
        go(fvalue, node.value.get(), x1)
    }

fun <A, B, C> assignAll(
    ftargets: Parser<A, List<B>, List<Expression>>,
    fvalue: Parser<B, C, Expression>
): Parser<A, List<C>, Assign> =
    Parser { node, x ->
        if (!node.value.isPresent)
            return@Parser Error()
        val x1 = ftargets.go(node.targets, x)
        multiGo(fvalue, node.value.get(), x1)
    }

fun <A, B, C> dict(
    fkeys: Parser<A, B, List<Expression>>,
    fvalues: Parser<B, C, List<Expression>>
): Parser<A, C, Dict> =
    Parser { node, x ->
        val x1 = fkeys.go(node.keys, x)
        go(fvalues, node.values, x1)
    }

fun <A, B> list(
    felts: Parser<A, B, List<Expression>>
): Parser<A, B, ListExpr> =
    Parser { node, x ->
        felts.go(node.elts, x)
    }

fun <A, B> set(
    felts: Parser<A, B, List<Expression>>
): Parser<A, B, Set> =
    Parser { node, x ->
        felts.go(node.elts, x)
    }

fun <A, B> tuple(
    felts: Parser<A, B, List<Expression>>
): Parser<A, B, Tuple> =
    Parser { node, x ->
        felts.go(node.elts, x)
    }

fun <A, B, C, D> augAssign(
    ftarget: Parser<A, B, Expression>,
    fop: Parser<B, C, Operator>,
    fvalue: Parser<C, D, Expression>
): Parser<A, D, AugAssign> =
    Parser { node, x ->
        val x1 = ftarget.go(node.target, x)
        val x2 = go(fop, node.op, x1)
        go(fvalue, node.value, x2)
    }

fun <A, B, C> binOp(
    fleft: Parser<A, B, Expression>,
    fright: Parser<B, C, Expression>
): Parser<A, C, BinOp> =
    Parser { node, x ->
        if (node.left == null || node.right == null)
            return@Parser Error()
        val x1 = fleft.go(node.left, x)
        go(fright, node.right, x1)
    }

fun <A, B, C> functionCall(
    fname: Parser<A, B, Name>,
    farguments: Parser<B, C, Arguments>
): Parser<A, C, Atom> =
    Parser { node, x ->
        if (!(node.atomElement is Name && node.trailers.size == 1 && node.trailers[0] is Arguments))
            return@Parser Error()
        val x1 = fname.go(node.atomElement as Name, x)
        go(farguments, node.trailers[0] as Arguments, x1)
    }

fun <A, B, C, D, E> arguments(
    fargs: Parser<A, B, List<Expression>>,
    fkeywords: Parser<B, C, List<Keyword>>,
    fstarredArgs: Parser<C, D, List<Expression>>,
    fdoubleStarredArgs: Parser<D, E, List<Keyword>>
): Parser<A, E, Arguments> =
    Parser { node, x ->
        val x1 = fargs.go(node.args ?: emptyList(), x)
        val x2 = go(fkeywords, node.keywords ?: emptyList(), x1)
        val x3 = go(fstarredArgs, node.starredArgs ?: emptyList(), x2)
        go(fdoubleStarredArgs, node.doubleStarredArgs ?: emptyList(), x3)
    }

fun <A, B, C> atom(
    fatomElement: Parser<A, B, Expression>,
    ftrailers: Parser<B, C, List<Expression>>
): Parser<A, C, Atom> =
    Parser { node, x ->
        val x1 = fatomElement.go(node.atomElement, x)
        go(ftrailers, node.trailers, x1)
    }

fun <A, B, N> list1(
    felem1: Parser<A, B, N>
): Parser<A, B, List<N>> =
    Parser { node, x ->
        if (node.size != 1)
            return@Parser Error()
        felem1.go(node[0], x)
    }

fun <A, B, C, N> list2(
    felem1: Parser<A, B, N>,
    felem2: Parser<B, C, N>
): Parser<A, C, List<N>> =
    Parser { node, x ->
        if (node.size != 2)
            return@Parser Error()
        val x1 = felem1.go(node[0], x)
        go(felem2, node[1], x1)
    }

fun <A, B, N> first(
    felem: Parser<A, B, N>
): Parser<A, B, List<N>> =
    Parser { node, x ->
        if (node.isEmpty())
            return@Parser Error()
        felem.go(node[0], x)
    }

fun <A, B> index(
    felem: Parser<A, B, Expression>
): Parser<A, B, Subscript> =
    Parser { node, x ->
        if (node.slice !is Index)
            return@Parser Error()
        felem.go((node.slice as Index).value, x)
    }

fun <A, B> attribute(
    fattributeId: Parser<A, B, String>
): Parser<A, B, Attribute> =
    Parser { node, x -> fattributeId.go(node.attr.name, x) }

fun <A, B, C> classField(
    fname: Parser<A, B, Name>,
    fattributeId: Parser<B, C, String>
): Parser<A, C, Atom> =
    atom(refl(fname), list1(refl(attribute(fattributeId))))

fun <A, B, C, D> classMethod(
    fname: Parser<A, B, Name>,
    fattributeId: Parser<B, C, String>,
    farguments: Parser<C, D, Arguments>
):Parser<A, D, Atom> =
    atom(refl(fname), list2(refl(attribute(fattributeId)), refl(farguments)))

fun <A, B, N> goWithMatches(
    pat: (N, A) -> Result<Pair<B, Int>>,
    node: N,
    x: Result<Pair<A, Int>>
): Result<Pair<B, Int>> =
    when (x) {
        is Error -> Error()
        is Match -> {
            when (val x1 = pat(node, x.value.first)) {
                is Error -> Error()
                is Match -> Match(Pair(x1.value.first, x1.value.second + x.value.second))
            }
        }
    }

fun <A> opExpr(
    fatomMatch: Parser<A, A, Expression>,
    fatomExtra: Parser<A, A, Expression>
): Parser<A, A, Expression> {
    fun innerGo(node: Expression, x: A): Result<Pair<A, Int>> {
        val y = fatomMatch.go(node, x)
        if (y is Match)
            return Match(Pair(y.value, 1))

        val z = fatomExtra.go(node, x)
        if (z is Match)
            return Match(Pair(z.value, 0))

        return when (node) {
            is BinOp -> {
                val x1 = innerGo(node.left, x)
                goWithMatches(::innerGo, node.right, x1)
            }
            is UnaryOp -> innerGo(node.expression, x)
            else -> Error()
        }
    }
    return Parser { node, x ->
        when (val x1 = innerGo(node, x)) {
            is Error -> Error()
            is Match -> if (x1.value.second == 0) Error() else Match(x1.value.first)
        }
    }
}

fun <A, B, N> any(felem: Parser<A, B, N>): Parser<A, B, List<N>> =
    Parser { node, x ->
        for (elem in node) {
            val x1 = felem.go(elem, x)
            if (x1 is Match)
                return@Parser x1
        }
        return@Parser Error()
    }

fun <A, B, N> allMatches(felem: Parser<A, B, N>): Parser<A, List<B>, List<N>> =
    Parser { node, x ->
        val res = mutableListOf<B>()
        for (elem in node) {
            val x1 = felem.go(elem, x)
            if (x1 is Match)
                res.add(x1.value)
        }
        Match(res)
    }

fun <A, B, N> anyIndexed(felem: Parser<A, B, N>): Parser<(Int) -> A, B, List<N>> =
    Parser { node, x ->
        node.forEachIndexed { index, elem ->
            val x1 = felem.go(elem, x(index))
            if (x1 is Match)
                return@Parser x1
        }
        return@Parser Error()
    }

fun <A, B, N> or(pat1: Parser<A, B, N>, pat2: Parser<A, B, N>): Parser<A, B, N> =
    Parser { node, x ->
        val x1 = pat1.go(node, x)
        when (x1) {
            is Match -> x1
            is Error -> pat2.go(node, x)
        }
    }

fun <A, B, N> reject(): Parser<A, B, N> = Parser { _, _ -> Error() }

fun <A, B, C, N> map0(pat: Parser<A, B, N>, value: C): Parser<(C) -> A, B, N> =
    Parser { node, x: (C) -> A ->
        pat.go(node, x(value))
    }

fun <A, B, C, D, N> map1(pat: Parser<(A) -> B, C, N>, f: (A) -> D): Parser<(D) -> B, C, N> =
    Parser { node, x: (D) -> B ->
        pat.go(node) { y -> x(f(y)) }
    }

fun <A, B, C, D, N> swap(pat: Parser<(A) -> (B) -> C, D, N>): Parser<(B) -> (A) -> C, D, N> =
    Parser { node, f: (B) -> (A) -> C ->
        pat.go(node) { x -> { y -> f(y)(x) } }
    }