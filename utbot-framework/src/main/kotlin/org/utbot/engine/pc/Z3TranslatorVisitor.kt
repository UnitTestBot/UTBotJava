package org.utbot.engine.pc

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.types.TypeRegistry
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
import org.utbot.engine.types.TypeRegistry.Companion.numberOfTypes
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.maxTypeNumberForEnumeration
import org.utbot.framework.UtSettings.useBitVecBasedTypeSystem
import java.util.IdentityHashMap
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

    override fun visit(expr: UtBvNotExpression): Expr = expr.run {
        z3Context.mkBVNot(translate(variable.expr) as BitVecExpr)
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
     * @see UtSettings.maxTypeNumberForEnumeration
     */
    override fun visit(expr: UtIsExpression): Expr = expr.run {
        val symNumDimensions = translate(typeRegistry.symNumDimensions(addr)) as BitVecExpr
        val symTypeId = translate(typeRegistry.symTypeId(addr)) as BitVecExpr

        val constraints = mutableListOf<BoolExpr>()

        // TODO remove it JIRA:1321
        val filteredPossibleTypes = workaround(WorkaroundReason.HACK) { typeStorage.filterInappropriateTypes() }

        if (filteredPossibleTypes.size > UtSettings.maxNumberOfTypesToEncode) {
            return@run z3Context.mkTrue()
        }

        val exprBaseType = expr.type.baseType
        val numDimensions = z3Context.mkBV(expr.type.numDimensions, Int.SIZE_BITS)

        constraints += if (exprBaseType.isJavaLangObject()) {
            z3Context.mkBVSGE(symNumDimensions, numDimensions)
        } else {
            z3Context.mkEq(symNumDimensions, numDimensions)
        }

        constraints += encodePossibleTypes(symTypeId, filteredPossibleTypes)

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
            val genericNumDimensions = translate(typeRegistry.genericNumDimensions(addr, i)) as BitVecExpr

            val possibleConcreteTypes = types[i].possibleConcreteTypes
            val leastCommonType = types[i].leastCommonType

            val numDimensions = z3Context.mkBV(leastCommonType.numDimensions, Int.SIZE_BITS)

            if (possibleConcreteTypes.size > UtSettings.maxNumberOfTypesToEncode) continue

            constraints += if (leastCommonType.isJavaLangObject()) {
                z3Context.mkBVSGE(genericNumDimensions, numDimensions)
            } else {
                z3Context.mkEq(genericNumDimensions, numDimensions)
            }

            constraints += encodePossibleTypes(symType, possibleConcreteTypes)
        }

        z3Context.mkOr(
            z3Context.mkAnd(*constraints.toTypedArray()),
            z3Context.mkEq(translate(expr.addr), translate(nullObjectAddr))
        )
    }

    private fun encodePossibleTypes(symType: Expr, possibleConcreteTypes: Collection<Type>): BoolExpr {
        val possibleBaseTypes = possibleConcreteTypes.map { it.baseType }

        return if (!useBitVecBasedTypeSystem || possibleConcreteTypes.size < maxTypeNumberForEnumeration) {
            z3Context.mkOr(
                *possibleBaseTypes
                    .map {
                        val typeId = typeRegistry.findTypeId(it)
                        val typeIdBv = z3Context.mkBV(typeId, Int.SIZE_BITS)
                        z3Context.mkEq(typeIdBv, symType)
                    }
                    .toTypedArray()
            )
        } else {
            val shiftedExpression = z3Context.mkBVSHL(
                z3Context.mkZeroExt(numberOfTypes - 1, z3Context.mkBV(1, 1)),
                z3Context.mkZeroExt(numberOfTypes - Int.SIZE_BITS, symType as BitVecExpr)
            )

            val bitVecString = typeRegistry.constructBitVecString(possibleBaseTypes)
            val possibleTypesBitVector = z3Context.mkBV(bitVecString, numberOfTypes)

            z3Context.mkEq(
                z3Context.mkBVAND(shiftedExpression, z3Context.mkBVNot(possibleTypesBitVector)),
                z3Context.mkBV(0, numberOfTypes)
            )
        }
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

    private fun Context.mkBV2Int(expr: UtExpression): IntExpr =
        if (expr is UtBvLiteral) {
            mkInt(expr.value as Long)
        } else {
            mkBV2Int(translate(expr) as BitVecExpr, true)
        }

    override fun visit(expr: UtArrayInsert): Expr = error("translate of UtArrayInsert expression")

    override fun visit(expr: UtArrayInsertRange): Expr = error("translate of UtArrayInsertRange expression")

    override fun visit(expr: UtArrayRemove): Expr = error("translate of UtArrayRemove expression")

    override fun visit(expr: UtArrayRemoveRange): Expr = error("translate of UtArrayRemoveRange expression")

    override fun visit(expr: UtArraySetRange): Expr = error("translate of UtArraySetRange expression")

    override fun visit(expr: UtArrayShiftIndexes): Expr = error("translate of UtArrayShiftIndexes expression")

    override fun visit(expr: UtArrayApplyForAll): Expr = error("translate of UtArrayApplyForAll expression")


}