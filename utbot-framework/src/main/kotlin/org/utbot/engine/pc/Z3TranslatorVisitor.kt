package org.utbot.engine.pc

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.MAX_STRING_LENGTH_SIZE_BITS
import org.utbot.engine.TypeRegistry
import org.utbot.engine.TypeStorage
import org.utbot.engine.baseType
import org.utbot.engine.defaultValue
import org.utbot.engine.isJavaLangObject
import org.utbot.engine.nullObjectAddr
import org.utbot.engine.numDimensions
import org.utbot.engine.z3.convertVar
import org.utbot.engine.z3.makeBV
import org.utbot.engine.z3.makeFP
import org.utbot.engine.z3.negate
import com.microsoft.z3.ArrayExpr
import com.microsoft.z3.BitVecExpr
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import com.microsoft.z3.FPSort
import com.microsoft.z3.IntExpr
import com.microsoft.z3.Model
import com.microsoft.z3.SeqExpr
import com.microsoft.z3.mkSeqNth
import java.util.IdentityHashMap
import soot.ByteType
import soot.CharType
import soot.PrimType
import soot.RefType
import soot.Type


open class Z3TranslatorVisitor(
    private val z3Context: Context,
    private val typeRegistry: TypeRegistry
) : UtExpressionVisitor<Expr> {

    //thread-safe
    private val expressionTranslationCache = IdentityHashMap<UtExpression, Expr>()

    fun evaluator(model: Model): Z3EvaluatorVisitor = Z3EvaluatorVisitor(model, this)

    /**
     * Translate [expr] from [UtExpression] to [Expr]
     */
    fun translate(expr: UtExpression): Expr =
        expressionTranslationCache.getOrPut(expr) { expr.accept(this) }

    fun lookUpCache(expr: UtExpression): Expr? = expressionTranslationCache[expr]

    fun withContext(block: Context.() -> Expr): Expr {
        return z3Context.block()
    }

    override fun visit(expr: UtArraySelectExpression): Expr = expr.run {
        z3Context.mkSelect(translate(arrayExpression) as ArrayExpr, translate(index))
    }

    /**
     * Creates a const array filled with a fixed value
     */
    override fun visit(expr: UtConstArrayExpression): Expr = expr.run {
        z3Context.mkConstArray(sort.indexSort.toZ3Sort(z3Context), translate(constValue))
    }

    override fun visit(expr: UtMkArrayExpression): Expr = expr.run {
        z3Context.run { mkConst(name, sort.toZ3Sort(this)) } as ArrayExpr
    }

    override fun visit(expr: UtArrayMultiStoreExpression): Expr = expr.run {
        stores.fold(translate(initial) as ArrayExpr) { acc, (index, item) ->
            z3Context.mkStore(acc, translate(index), translate(item))
        }
    }

    override fun visit(expr: UtBvLiteral): Expr = expr.run { z3Context.makeBV(value, size) }

    override fun visit(expr: UtBvConst): Expr = expr.run { z3Context.mkBVConst(name, size) }

    override fun visit(expr: UtAddrExpression): Expr = expr.run { translate(internal) }

    override fun visit(expr: UtFpLiteral): Expr =
        expr.run { z3Context.makeFP(value, sort.toZ3Sort(z3Context) as FPSort) }

    override fun visit(expr: UtFpConst): Expr = expr.run { z3Context.mkConst(name, sort.toZ3Sort(z3Context)) }

    override fun visit(expr: UtOpExpression): Expr = expr.run {
        val leftResolve = translate(left.expr).z3Variable(left.type)
        val rightResolve = translate(right.expr).z3Variable(right.type)
        operator.delegate(z3Context, leftResolve, rightResolve)
    }

    override fun visit(expr: UtTrue): Expr = expr.run { z3Context.mkBool(true) }

    override fun visit(expr: UtFalse): Expr = expr.run { z3Context.mkBool(false) }

    override fun visit(expr: UtEqExpression): Expr = expr.run { z3Context.mkEq(translate(left), translate(right)) }

    override fun visit(expr: UtBoolConst): Expr = expr.run { z3Context.mkBoolConst(name) }

    override fun visit(expr: NotBoolExpression): Expr =
        expr.run { z3Context.mkNot(translate(expr.expr) as BoolExpr) }

    override fun visit(expr: UtOrBoolExpression): Expr = expr.run {
        z3Context.mkOr(*exprs.map { translate(it) as BoolExpr }.toTypedArray())
    }

    override fun visit(expr: UtAndBoolExpression): Expr = expr.run {
        z3Context.mkAnd(*exprs.map { translate(it) as BoolExpr }.toTypedArray())
    }

    override fun visit(expr: UtAddNoOverflowExpression): Expr = expr.run {
        z3Context.mkBVAddNoOverflow(translate(expr.left) as BitVecExpr, translate(expr.right) as BitVecExpr, true)
    }

    override fun visit(expr: UtSubNoOverflowExpression): Expr = expr.run {
        // For some reason mkBVSubNoOverflow does not take "signed" as an argument, yet still works for signed integers
        z3Context.mkBVSubNoOverflow(translate(expr.left) as BitVecExpr, translate(expr.right) as BitVecExpr) // , true)
    }

    override fun visit(expr: UtNegExpression): Expr = expr.run {
        negate(z3Context, translate(variable.expr).z3Variable(variable.type))
    }

    override fun visit(expr: UtCastExpression): Expr = expr.run {
        val z3var = translate(variable.expr).z3Variable(variable.type)
        z3Context.convertVar(z3var, type).expr
    }

    override fun visit(expr: UtBoolOpExpression): Expr = expr.run {
        val leftResolve = translate(left.expr).z3Variable(left.type)
        val rightResolve = translate(right.expr).z3Variable(right.type)
        operator.delegate(z3Context, leftResolve, rightResolve)
    }

    /**
     * Translation method for an UtIsExpression. A way for depends on the amount of possible types for the given expr.
     *
     * If this number less than MAX_TYPE_NUMBER_FOR_ENUMERATION, we use enumeration:
     * we build an expression 'or (symType == t1) (symType == t2) (symType == t2) …'.
     *
     * But when this number greater than MAX_TYPE_NUMBER_FOR_ENUMERATION or equals to it, we use bit-vectors:
     * we build a bit-vector with length expr.numberOfTypes. One on some position k means
     * that object can be an instance of some type with id k (zero means it cannot be).
     * Thus, we create such bit-vector for given possibleTypes from the expr.typeStorage, let's call it typeVector.
     * Now we can add type condition for expr's typeId: (1 << typeId) & ~typeVector == 0
     *
     * The reason, why we cannot just translate everything in one way is performance. It is too expensive to
     * use bit-vector all the time, but enumeration can cause significant problems, for example, for Objects.
     *
     * @param expr  the type expression
     * @return  the type expression translated into z3 assertions
     * @see UtIsExpression
     * @see MAX_TYPE_NUMBER_FOR_ENUMERATION
     */
    override fun visit(expr: UtIsExpression): Expr = expr.run {
        val symNumDimensions = translate(typeRegistry.symNumDimensions(addr)) as BitVecExpr
        val symTypeId = translate(typeRegistry.symTypeId(addr)) as BitVecExpr

        val constraints = mutableListOf<BoolExpr>()

        // TODO remove it JIRA:1321
        val filteredPossibleTypes = workaround(WorkaroundReason.HACK) { typeStorage.filterInappropriateTypes() }

        // add constraints for typeId
        if (typeStorage.possibleConcreteTypes.size < MAX_TYPE_NUMBER_FOR_ENUMERATION) {
            val symType = translate(typeRegistry.symTypeId(addr))
            val possibleBaseTypes = filteredPossibleTypes.map { it.baseType }

            val typeConstraint = z3Context.mkOr(
                *possibleBaseTypes
                    .map { z3Context.mkEq(z3Context.mkBV(typeRegistry.findTypeId(it), Int.SIZE_BITS), symType) }
                    .toTypedArray()
            )

            constraints += typeConstraint
        } else {
            val shiftedExpression = z3Context.mkBVSHL(
                z3Context.mkZeroExt(numberOfTypes - 1, z3Context.mkBV(1, 1)),
                z3Context.mkZeroExt(numberOfTypes - Int.SIZE_BITS, symTypeId)
            )

            val bitVecString = typeRegistry.constructBitVecString(filteredPossibleTypes)
            val possibleTypesBitVector = z3Context.mkBV(bitVecString, numberOfTypes)

            val typeConstraint = z3Context.mkEq(
                z3Context.mkBVAND(shiftedExpression, z3Context.mkBVNot(possibleTypesBitVector)),
                z3Context.mkBV(0, numberOfTypes)
            )

            constraints += typeConstraint
        }

        val exprBaseType = expr.type.baseType
        val numDimensions = z3Context.mkBV(expr.type.numDimensions, Int.SIZE_BITS)

        constraints += if (exprBaseType.isJavaLangObject()) {
            z3Context.mkBVSGE(symNumDimensions, numDimensions)
        } else {
            z3Context.mkEq(symNumDimensions, numDimensions)
        }

        z3Context.mkAnd(*constraints.toTypedArray())
    }

    // TODO REMOVE IT JIRA:1321
    private fun TypeStorage.filterInappropriateTypes(): Collection<Type> {
        val filteredTypes = if (!leastCommonType.isJavaLangObject()) {
            possibleConcreteTypes
        } else {
            possibleConcreteTypes.filter {
                val baseType = it.baseType
                if (baseType is PrimType) return@filter true

                baseType as RefType
                "org.utbot.engine.overrides.collections" !in baseType.sootClass.packageName
            }
        }

        return filteredTypes.ifEmpty { possibleConcreteTypes }

    }

    override fun visit(expr: UtGenericExpression): Expr = expr.run {
        val constraints = mutableListOf<BoolExpr>()
        for (i in types.indices) {
            val symType = translate(typeRegistry.genericTypeId(addr, i))

            if (types[i].leastCommonType.isJavaLangObject()) {
                 continue
            }

            val possibleBaseTypes = types[i].possibleConcreteTypes.map { it.baseType }

            val typeConstraint = z3Context.mkOr(
                *possibleBaseTypes.map {
                    z3Context.mkEq(
                        z3Context.mkBV(typeRegistry.findTypeId(it), Int.SIZE_BITS),
                        symType
                    )
                }.toTypedArray()
            )

            constraints += typeConstraint
        }

        z3Context.mkOr(
            z3Context.mkAnd(*constraints.toTypedArray()),
            z3Context.mkEq(translate(expr.addr), translate(nullObjectAddr))
        )
    }

    override fun visit(expr: UtIsGenericTypeExpression): Expr = expr.run {
        val symType = translate(typeRegistry.symTypeId(addr))
        val symNumDimensions = translate(typeRegistry.symNumDimensions(addr))

        val genericSymType = translate(typeRegistry.genericTypeId(baseAddr, parameterTypeIndex))
        val genericNumDimensions = translate(typeRegistry.genericNumDimensions(baseAddr, parameterTypeIndex))

        val dimensionsConstraint = z3Context.mkEq(symNumDimensions, genericNumDimensions)

        val equalTypeConstraint = z3Context.mkAnd(
            z3Context.mkEq(symType, genericSymType),
            dimensionsConstraint
        )

        val typeConstraint = z3Context.mkOr(
            equalTypeConstraint,
            z3Context.mkEq(translate(expr.addr), translate(nullObjectAddr))
        )

        z3Context.mkAnd(typeConstraint, dimensionsConstraint)
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): Expr = expr.run {
        val constraints = mutableListOf<BoolExpr>()
        for ((i, j) in indexMapping) {
            val firstSymType = translate(typeRegistry.genericTypeId(firstAddr, i))
            val secondSymType = translate(typeRegistry.genericTypeId(secondAddr, j))
            constraints += z3Context.mkEq(firstSymType, secondSymType)

            val firstSymNumDimensions = translate(typeRegistry.genericNumDimensions(firstAddr, i))
            val secondSymNumDimensions = translate(typeRegistry.genericNumDimensions(secondAddr, j))
            constraints += z3Context.mkEq(firstSymNumDimensions, secondSymNumDimensions)
        }
        z3Context.mkAnd(*constraints.toTypedArray())
    }

    override fun visit(expr: UtInstanceOfExpression): Expr =
        expr.run { translate(expr.constraint) }

    override fun visit(expr: UtIteExpression): Expr =
        expr.run { z3Context.mkITE(translate(condition) as BoolExpr, translate(thenExpr), translate(elseExpr)) }

    override fun visit(expr: UtMkTermArrayExpression): Expr = expr.run {
        z3Context.run {
            val ourArray = translate(expr.array) as ArrayExpr
            val arraySort = expr.array.sort
            val defaultValue = expr.defaultValue ?: arraySort.itemSort.defaultValue
            val translated = translate(defaultValue)

            mkEq(translated, mkTermArray(ourArray))
        }
    }

    override fun visit(expr: UtStringConst): Expr = expr.run { z3Context.mkConst(name, sort.toZ3Sort(z3Context)) }

    override fun visit(expr: UtConcatExpression): Expr =
        expr.run { z3Context.mkConcat(*parts.map { translate(it) as SeqExpr }.toTypedArray()) }

    override fun visit(expr: UtConvertToString): Expr = expr.run {
        when (expression) {
            is UtBvLiteral -> z3Context.mkString(expression.value.toString())
            else -> {
                val intValue = z3Context.mkBV2Int(translate(expression) as BitVecExpr, true)
                z3Context.intToString(intValue)
            }
        }
    }

    override fun visit(expr: UtStringToInt): Expr = expr.run {
        val intValue = z3Context.stringToInt(translate(expression))
        z3Context.mkInt2BV(size, intValue)
    }

    override fun visit(expr: UtStringLength): Expr = expr.run {
        z3Context.mkInt2BV(MAX_STRING_LENGTH_SIZE_BITS, z3Context.mkLength(translate(string) as SeqExpr))
    }

    override fun visit(expr: UtStringPositiveLength): Expr = expr.run {
        z3Context.mkGe(z3Context.mkLength(translate(string) as SeqExpr), z3Context.mkInt(0))
    }

    private fun Context.mkBV2Int(expr: UtExpression): IntExpr =
        if (expr is UtBvLiteral) {
            mkInt(expr.value as Long)
        } else {
            mkBV2Int(translate(expr) as BitVecExpr, true)
        }


    override fun visit(expr: UtStringCharAt): Expr = expr.run {
        val charAtExpr = z3Context.mkSeqNth(translate(string) as SeqExpr, z3Context.mkBV2Int(index))
        val z3var = charAtExpr.z3Variable(ByteType.v())
        z3Context.convertVar(z3var, CharType.v()).expr
    }

    override fun visit(expr: UtStringEq): Expr = expr.run { z3Context.mkEq(translate(left), translate(right)) }

    override fun visit(expr: UtSubstringExpression): Expr = expr.run {
        z3Context.mkExtract(
            translate(string) as SeqExpr,
            z3Context.mkBV2Int(beginIndex),
            z3Context.mkBV2Int(length)
        )
    }

    override fun visit(expr: UtReplaceExpression): Expr = expr.run {
        workaround(WorkaroundReason.HACK) { // mkReplace replaces first occasion only
            z3Context.mkReplace(
                translate(string) as SeqExpr,
                translate(regex) as SeqExpr,
                translate(replacement) as SeqExpr
            )
        }
    }

    // Attention, prefix is a first argument!
    override fun visit(expr: UtStartsWithExpression): Expr = expr.run {
        z3Context.mkPrefixOf(translate(prefix) as SeqExpr, translate(string) as SeqExpr)
    }

    // Attention, suffix is a first argument!
    override fun visit(expr: UtEndsWithExpression): Expr = expr.run {
        z3Context.mkSuffixOf(translate(suffix) as SeqExpr, translate(string) as SeqExpr)
    }

    override fun visit(expr: UtIndexOfExpression): Expr = expr.run {
        z3Context.mkInt2BV(
            MAX_STRING_LENGTH_SIZE_BITS,
            z3Context.mkIndexOf(translate(string) as SeqExpr, translate(substring) as SeqExpr, z3Context.mkInt(0))
        )
    }

    override fun visit(expr: UtContainsExpression): Expr = expr.run {
        z3Context.mkGe(
            z3Context.mkIndexOf(translate(string) as SeqExpr, translate(substring) as SeqExpr, z3Context.mkInt(0)),
            z3Context.mkInt(0)
        )
    }

    override fun visit(expr: UtToStringExpression): Expr = expr.run {
        z3Context.mkITE(translate(isNull) as BoolExpr, z3Context.mkString("null"), translate(notNullExpr))

    }

    override fun visit(expr: UtSeqLiteral): Expr = expr.run { z3Context.mkString(value) }

    companion object {
        const val MAX_TYPE_NUMBER_FOR_ENUMERATION = 64
    }

    override fun visit(expr: UtArrayInsert): Expr = error("translate of UtArrayInsert expression")

    override fun visit(expr: UtArrayInsertRange): Expr = error("translate of UtArrayInsertRange expression")

    override fun visit(expr: UtArrayRemove): Expr = error("translate of UtArrayRemove expression")

    override fun visit(expr: UtArrayRemoveRange): Expr = error("translate of UtArrayRemoveRange expression")

    override fun visit(expr: UtArraySetRange): Expr = error("translate of UtArraySetRange expression")

    override fun visit(expr: UtArrayShiftIndexes): Expr = error("translate of UtArrayShiftIndexes expression")

    override fun visit(expr: UtArrayApplyForAll): Expr = error("translate of UtArrayApplyForAll expression")

    override fun visit(expr: UtStringToArray): Expr = error("translate of UtStringToArray expression")

    override fun visit(expr: UtArrayToString): Expr = error("translate of UtArrayToString expression")
}