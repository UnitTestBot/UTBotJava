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

sealed class Result<T>
class Match<T>(val value: T): Result<T>()
class Error<T> : Result<T>()

class Parser<A, B, N> (
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

fun <A, B, N> any(felem: Parser<A, B, N>): Parser<A, B, List<N>> =
    Parser { node, x ->
        for (elem in node) {
            val x1 = felem.go(elem, x)
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