package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.operators.binaryops.BinOp
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.Assign
import io.github.danielnaczo.python3parser.model.stmts.smallStmts.assignStmts.AugAssign
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.python.PythonMethod

object ConstantSuggester {
    fun suggestBasedOnConstants(method: PythonMethod, indices: List<Int>): List<List<ClassId>> {
        val params = method.arguments
        return params.mapIndexed { index, param ->
            if (indices.contains(index)) {
                val visitor = MatchVisitor(param.name)
                val types = mutableSetOf<Storage>()
                visitor.visitFunctionDef(method.ast(), types)
                types.map { ClassId(it.value) }
            } else {
                listOf(param.type)
            }
        }
    }

    data class Storage(val value: String)

    private class MatchVisitor(val paramName: String): ModifierVisitor<MutableCollection<Storage>>() {

        fun <A, N> namePat(): Parser<A, A, N> = refl(name(equal(paramName)))
        fun <A, N> valuePat(): Parser<(Storage) -> A, A, N> {
            val consts = listOf<Parser<(Storage) -> A, A, N>>(
                map0(refl(num(drop())), Storage("int")),
                map0(refl(str(drop())), Storage("str")),
                map0(refl(true_()), Storage("bool")),
                map0(refl(false_()), Storage("bool")),
                // map0(refl(none()), Storage("None")),
                map0(refl(dict(drop(), drop())), Storage("dict")),
                map0(refl(list(drop())), Storage("list")),
                map0(refl(set(drop())), Storage("set")),
                map0(refl(tuple(drop())), Storage("tuple"))
            )
            return consts.reduce { acc, elem -> or(acc, elem) }
        }

        private fun <N> parseAndPut(
            collection: MutableCollection<Storage>,
            pat: Parser<(Storage) -> Storage?, Storage?, N>,
            ast: N
        ) {
            parse(
                pat,
                onError = null,
                ast
            ) { it }?.let {
                collection.add(it)
            }
        }

        override fun visitAssign(ast: Assign, param: MutableCollection<Storage>): AST {
            parseAndPut(param, assign(ftargets = any(namePat()), fvalue = valuePat()), ast)
            return super.visitAssign(ast, param)
        }

        override fun visitAugAssign(ast: AugAssign, param: MutableCollection<Storage>): AST {
            parseAndPut(
                param,
                augAssign(ftarget = namePat(), fvalue = valuePat(), fop = drop()),
                ast
            )
            return super.visitAugAssign(ast, param)
        }

        override fun visitBinOp(ast: BinOp, param: MutableCollection<Storage>): AST {
            parseAndPut(
                param,
                or(
                    binOp(fleft = namePat(), fright = valuePat()),
                    binOp(fleft = valuePat(), fright = namePat())
                ),
                ast
            )
            return super.visitBinOp(ast, param)
        }
    }
}