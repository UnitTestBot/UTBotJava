package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.expr.Expression
import io.github.danielnaczo.python3parser.model.expr.atoms.*
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.Attribute
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Arguments
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.arguments.Keyword
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.subscripts.Index
import io.github.danielnaczo.python3parser.model.expr.atoms.trailers.subscripts.Subscript
import io.github.danielnaczo.python3parser.model.expr.datastructures.Dict
import io.github.danielnaczo.python3parser.model.expr.datastructures.ListExpr
import io.github.danielnaczo.python3parser.model.expr.datastructures.Set
import io.github.danielnaczo.python3parser.model.expr.datastructures.Tuple
import io.github.danielnaczo.python3parser.model.expr.operators.Operator
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.BinOp
import io.github.danielnaczo.python3parser.model.expr.operators.unaryops.UnaryOp
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AugAssign
import org.apache.commons.lang3.math.NumberUtils
import java.math.BigInteger

sealed class Result<T>
class Match<T>(val value: T) : Result<T>()
class Error<T> : Result<T>()

open class Pattern<A, B, N>(
    val go: (N, A) -> Result<B>
)

fun <A, B, N> parse(pat: Pattern<A, B, N>, onError: B, node: N, x: A): B {
    return when (val result = pat.go(node, x)) {
        is Match -> result.value
        is Error -> onError
    }
}

inline fun <A, B, reified N, M> refl(pat: Pattern<A, B, N>): Pattern<A, B, M> =
    Pattern { node, x ->
        when (node) {
            is N -> pat.go(node, x)
            else -> Error()
        }
    }

fun <A, N> drop(): Pattern<A, A, N> =
    Pattern { _, x -> Match(x) }

fun <A, N> apply(): Pattern<(N) -> A, A, N> =
    Pattern { node, x -> Match(x(node)) }

fun <A, B> name(fid: Pattern<A, B, String>): Pattern<A, B, Name> =
    Pattern { node, x ->
        fid.go(node.id.name, x)
    }

fun <A> int(): Pattern<A, A, String> =
    Pattern { node, x ->
        when (NumberUtils.createNumber(node)) {
            is Int -> Match(x)
            is Long -> Match(x)
            is BigInteger -> Match(x)
            else -> Error()
        }
    }

fun <A, B> num(fnum: Pattern<A, B, String>): Pattern<A, B, Num> = Pattern { node, x -> fnum.go(node.n, x) }
fun <A, B> str(fstr: Pattern<A, B, String>): Pattern<A, B, Str> = Pattern { node, x -> fstr.go(node.s, x) }
fun <A> true_(): Pattern<A, A, True> = Pattern { _, x -> Match(x) }
fun <A> false_(): Pattern<A, A, False> = Pattern { _, x -> Match(x) }
fun <A> none(): Pattern<A, A, None> = Pattern { _, x -> Match(x) }

fun <A, N> equal(value: N): Pattern<A, A, N> =
    Pattern { node, x ->
        if (node != value)
            Error()
        else
            Match(x)
    }

fun <A, B, N> go(y: Pattern<A, B, N>, node: N, x: Result<A>) =
    when (x) {
        is Match -> y.go(node, x.value)
        is Error -> Error()
    }

fun <A> toMatch(x: Result<List<A>>): Match<List<A>> =
    when (x) {
        is Error -> Match(emptyList())
        is Match -> x
    }

fun <A, B, N> multiGo(y: Pattern<A, B, N>, node: N, x: Result<List<A>>): Result<List<B>> {
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
    ftargets: Pattern<A, B, List<Expression>>,
    fvalue: Pattern<B, C, Expression>
): Pattern<A, C, Assign> =
    Pattern { node, x ->
        if (!node.value.isPresent)
            return@Pattern Error()
        val x1 = ftargets.go(node.targets, x)
        go(fvalue, node.value.get(), x1)
    }

fun <A, B, C> assignAll(
    ftargets: Pattern<A, List<B>, List<Expression>>,
    fvalue: Pattern<B, C, Expression>
): Pattern<A, List<C>, Assign> =
    Pattern { node, x ->
        if (!node.value.isPresent)
            return@Pattern Error()
        val x1 = ftargets.go(node.targets, x)
        multiGo(fvalue, node.value.get(), x1)
    }

fun <A, B, C> dict(
    fkeys: Pattern<A, B, List<Expression>>,
    fvalues: Pattern<B, C, List<Expression>>
): Pattern<A, C, Dict> =
    Pattern { node, x ->
        val x1 = fkeys.go(node.keys, x)
        go(fvalues, node.values, x1)
    }

fun <A, B> list(
    felts: Pattern<A, B, List<Expression>>
): Pattern<A, B, ListExpr> =
    Pattern { node, x ->
        felts.go(node.elts, x)
    }

fun <A, B> set(
    felts: Pattern<A, B, List<Expression>>
): Pattern<A, B, Set> =
    Pattern { node, x ->
        felts.go(node.elts, x)
    }

fun <A, B> tuple(
    felts: Pattern<A, B, List<Expression>>
): Pattern<A, B, Tuple> =
    Pattern { node, x ->
        felts.go(node.elts, x)
    }

fun <A, B, C, D> augAssign(
    ftarget: Pattern<A, B, Expression>,
    fop: Pattern<B, C, Operator>,
    fvalue: Pattern<C, D, Expression>
): Pattern<A, D, AugAssign> =
    Pattern { node, x ->
        val x1 = ftarget.go(node.target, x)
        val x2 = go(fop, node.op, x1)
        go(fvalue, node.value, x2)
    }

fun <A, B, C> binOp(
    fleft: Pattern<A, B, Expression>,
    fright: Pattern<B, C, Expression>
): Pattern<A, C, BinOp> =
    Pattern { node, x ->
        if (node.left == null || node.right == null)
            return@Pattern Error()
        val x1 = fleft.go(node.left, x)
        go(fright, node.right, x1)
    }

fun <A, B, C, D, E> arguments(
    fargs: Pattern<A, B, List<Expression>>,
    fkeywords: Pattern<B, C, List<Keyword>>,
    fstarredArgs: Pattern<C, D, List<Expression>>,
    fdoubleStarredArgs: Pattern<D, E, List<Keyword>>
): Pattern<A, E, Arguments> =
    Pattern { node, x ->
        val x1 = fargs.go(node.args ?: emptyList(), x)
        val x2 = go(fkeywords, node.keywords ?: emptyList(), x1)
        val x3 = go(fstarredArgs, node.starredArgs ?: emptyList(), x2)
        go(fdoubleStarredArgs, node.doubleStarredArgs ?: emptyList(), x3)
    }

fun <A, B, C> atom(
    fatomElement: Pattern<A, B, Expression>,
    ftrailers: Pattern<B, C, List<Expression>>
): Pattern<A, C, Atom> =
    Pattern { node, x ->
        val x1 = fatomElement.go(node.atomElement, x)
        go(ftrailers, node.trailers, x1)
    }

fun <A, B, N> list1(
    felem1: Pattern<A, B, N>
): Pattern<A, B, List<N>> =
    Pattern { node, x ->
        if (node.size != 1)
            return@Pattern Error()
        felem1.go(node[0], x)
    }

fun <A, B, C, N> list2(
    felem1: Pattern<A, B, N>,
    felem2: Pattern<B, C, N>
): Pattern<A, C, List<N>> =
    Pattern { node, x ->
        if (node.size != 2)
            return@Pattern Error()
        val x1 = felem1.go(node[0], x)
        go(felem2, node[1], x1)
    }

fun <A, B, N> first(
    felem: Pattern<A, B, N>
): Pattern<A, B, List<N>> =
    Pattern { node, x ->
        if (node.isEmpty())
            return@Pattern Error()
        felem.go(node[0], x)
    }

fun <A, B> index(
    felem: Pattern<A, B, Expression>
): Pattern<A, B, Subscript> =
    Pattern { node, x ->
        if (node.slice !is Index)
            return@Pattern Error()
        felem.go((node.slice as Index).value, x)
    }

fun <A, B> attribute(
    fattributeId: Pattern<A, B, String>
): Pattern<A, B, Attribute> =
    Pattern { node, x -> fattributeId.go(node.attr.name, x) }

fun <A, B, C> classField(
    fname: Pattern<A, B, Name>,
    fattributeId: Pattern<B, C, String>
): Pattern<A, C, Atom> =
    atom(refl(fname), list1(refl(attribute(fattributeId))))

fun <A, B> attributesFromAtom(
    fattributes: Pattern<A, B, List<String>>
): Pattern<A, B, Atom> =
    Pattern { node, x ->
        val res = mutableListOf<String>()
        for (elem in node.trailers) {
            if (elem is Attribute)
                res.add(elem.attr.name)
        }
        fattributes.go(res, x)
    }

fun <A, B, C, D> classMethod(
    fname: Pattern<A, B, Name>,
    fattributeId: Pattern<B, C, String>,
    farguments: Pattern<C, D, Arguments>
): Pattern<A, D, Atom> =
    atom(refl(fname), list2(refl(attribute(fattributeId)), refl(farguments)))

fun <A, B, C> methodFromAtom(
    fattributeId: Pattern<A, B, String>,
    farguments: Pattern<B, C, Arguments>
): Pattern<A, C, Atom> =
    Pattern { node, x ->
        if (node.trailers.size < 2 || node.trailers.last() !is Arguments)
            return@Pattern Error()
        val methodName = node.trailers[node.trailers.size - 2]
        if (methodName !is Attribute)
            return@Pattern Error()
        val x1 = fattributeId.go(methodName.attr.name, x)
        go(farguments, node.trailers.last() as Arguments, x1)
    }

fun <A, B> nameWithPrefixFromAtom(
    fname: Pattern<A, B, String>
): Pattern<A, B, Atom> =
    Pattern { node, x ->
        if (node.atomElement !is Name)
            return@Pattern Error()
        var res = (node.atomElement as Name).id.name
        for (elem in node.trailers) {
            if (elem !is Attribute)
                break
            res += "." + elem.attr.name
        }
        fname.go(res, x)
    }

fun <A, B, C> functionCallWithoutPrefix(
    fname: Pattern<A, B, Name>,
    farguments: Pattern<B, C, Arguments>
): Pattern<A, C, Atom> =
    atom(refl(fname), list1(refl(farguments)))

fun <A, B, C, D> functionCallWithPrefix(
    fprefix: Pattern<A, B, List<Expression>>,
    fid: Pattern<B, C, String>,
    farguments: Pattern<C, D, Arguments>
): Pattern<A, D, Atom> =
    Pattern { node, x ->
        if (node.trailers.size == 0)
            return@Pattern Error()
        if (node.trailers.size == 1) {
            val x1 = fprefix.go(emptyList(), x)
            return@Pattern go(functionCallWithoutPrefix(name(fid), farguments), node, x1)
        }
        val prefix = listOf(node.atomElement) + node.trailers.dropLast(2)
        val x1 = fprefix.go(prefix, x)
        val x2 = go(refl(attribute(fid)), node.trailers[node.trailers.size - 2], x1)
        go(refl(farguments), node.trailers.last(), x2)
    }

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
    fatomMatch: Pattern<A, A, Expression>,
    fatomExtra: Pattern<A, A, Expression>
): Pattern<A, A, Expression> {
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
    return Pattern { node, x ->
        when (val x1 = innerGo(node, x)) {
            is Error -> Error()
            is Match -> if (x1.value.second == 0) Error() else Match(x1.value.first)
        }
    }
}

fun <A, B, N> any(felem: Pattern<A, B, N>): Pattern<A, B, List<N>> =
    Pattern { node, x ->
        for (elem in node) {
            val x1 = felem.go(elem, x)
            if (x1 is Match)
                return@Pattern x1
        }
        return@Pattern Error()
    }

fun <A, B, N> allMatches(felem: Pattern<A, B, N>): Pattern<A, List<B>, List<N>> =
    Pattern { node, x ->
        val res = mutableListOf<B>()
        for (elem in node) {
            val x1 = felem.go(elem, x)
            if (x1 is Match)
                res.add(x1.value)
        }
        Match(res)
    }

fun <A, B, N> anyIndexed(felem: Pattern<A, B, N>): Pattern<(Int) -> A, B, List<N>> =
    Pattern { node, x ->
        node.forEachIndexed { index, elem ->
            val x1 = felem.go(elem, x(index))
            if (x1 is Match)
                return@Pattern x1
        }
        return@Pattern Error()
    }

fun <A, B, N> or(pat1: Pattern<A, B, N>, pat2: Pattern<A, B, N>): Pattern<A, B, N> =
    Pattern { node, x ->
        val x1 = pat1.go(node, x)
        when (x1) {
            is Match -> x1
            is Error -> pat2.go(node, x)
        }
    }

fun <A, B, N> reject(): Pattern<A, B, N> = Pattern { _, _ -> Error() }

fun <A, B, C, N> map0(pat: Pattern<A, B, N>, value: C): Pattern<(C) -> A, B, N> =
    Pattern { node, x: (C) -> A ->
        pat.go(node, x(value))
    }

fun <A, B, C, D, N> map1(pat: Pattern<(A) -> B, C, N>, f: (A) -> D): Pattern<(D) -> B, C, N> =
    Pattern { node, x: (D) -> B ->
        pat.go(node) { y -> x(f(y)) }
    }

fun <A, B, C, D, N> swap(pat: Pattern<(A) -> (B) -> C, D, N>): Pattern<(B) -> (A) -> C, D, N> =
    Pattern { node, f: (B) -> (A) -> C ->
        pat.go(node) { x -> { y -> f(y)(x) } }
    }