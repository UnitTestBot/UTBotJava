package org.utbot.features

import org.utbot.engine.pc.NotBoolExpression
import org.utbot.engine.pc.UtAddNoOverflowExpression
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtAndBoolExpression
import org.utbot.engine.pc.UtArrayApplyForAll
import org.utbot.engine.pc.UtArrayInsert
import org.utbot.engine.pc.UtArrayInsertRange
import org.utbot.engine.pc.UtArrayMultiStoreExpression
import org.utbot.engine.pc.UtArrayRemove
import org.utbot.engine.pc.UtArrayRemoveRange
import org.utbot.engine.pc.UtArraySelectExpression
import org.utbot.engine.pc.UtArraySetRange
import org.utbot.engine.pc.UtArrayShiftIndexes
import org.utbot.engine.pc.UtArrayToString
import org.utbot.engine.pc.UtBoolConst
import org.utbot.engine.pc.UtBoolOpExpression
import org.utbot.engine.pc.UtBvConst
import org.utbot.engine.pc.UtBvLiteral
import org.utbot.engine.pc.UtCastExpression
import org.utbot.engine.pc.UtConcatExpression
import org.utbot.engine.pc.UtConstArrayExpression
import org.utbot.engine.pc.UtContainsExpression
import org.utbot.engine.pc.UtConvertToString
import org.utbot.engine.pc.UtEndsWithExpression
import org.utbot.engine.pc.UtEqExpression
import org.utbot.engine.pc.UtEqGenericTypeParametersExpression
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.UtExpressionVisitor
import org.utbot.engine.pc.UtFalse
import org.utbot.engine.pc.UtFpConst
import org.utbot.engine.pc.UtFpLiteral
import org.utbot.engine.pc.UtGenericExpression
import org.utbot.engine.pc.UtIndexOfExpression
import org.utbot.engine.pc.UtInstanceOfExpression
import org.utbot.engine.pc.UtIsExpression
import org.utbot.engine.pc.UtIsGenericTypeExpression
import org.utbot.engine.pc.UtIteExpression
import org.utbot.engine.pc.UtMkArrayExpression
import org.utbot.engine.pc.UtMkTermArrayExpression
import org.utbot.engine.pc.UtNegExpression
import org.utbot.engine.pc.UtOpExpression
import org.utbot.engine.pc.UtOrBoolExpression
import org.utbot.engine.pc.UtReplaceExpression
import org.utbot.engine.pc.UtSeqLiteral
import org.utbot.engine.pc.UtStartsWithExpression
import org.utbot.engine.pc.UtStringCharAt
import org.utbot.engine.pc.UtStringConst
import org.utbot.engine.pc.UtStringEq
import org.utbot.engine.pc.UtStringLength
import org.utbot.engine.pc.UtStringPositiveLength
import org.utbot.engine.pc.UtStringToArray
import org.utbot.engine.pc.UtStringToInt
import org.utbot.engine.pc.UtSubNoOverflowExpression
import org.utbot.engine.pc.UtSubstringExpression
import org.utbot.engine.pc.UtToStringExpression
import org.utbot.engine.pc.UtTrue
import org.utbot.utils.BinaryUtil
import java.util.IdentityHashMap
import java.util.UUID


class UtExpressionId: UtExpressionVisitor<Int> {

    fun getID(expr: UtExpression): Int = expr.accept(this)

    override fun visit(expr: UtArraySelectExpression): Int = 1
    override fun visit(expr: UtMkArrayExpression): Int = 2
    override fun visit(expr: UtArrayMultiStoreExpression): Int = 3
    override fun visit(expr: UtBvLiteral): Int = 4
    override fun visit(expr: UtBvConst): Int = 5
    override fun visit(expr: UtAddrExpression): Int = 6
    override fun visit(expr: UtFpLiteral): Int = 7
    override fun visit(expr: UtFpConst): Int = 8
    override fun visit(expr: UtOpExpression): Int = 9
    override fun visit(expr: UtTrue): Int = 10
    override fun visit(expr: UtFalse): Int = 11
    override fun visit(expr: UtEqExpression): Int = 12
    override fun visit(expr: UtBoolConst): Int = 13
    override fun visit(expr: NotBoolExpression): Int = 14
    override fun visit(expr: UtOrBoolExpression): Int = 15
    override fun visit(expr: UtAndBoolExpression): Int = 16
    override fun visit(expr: UtNegExpression): Int = 17
    override fun visit(expr: UtCastExpression): Int = 18
    override fun visit(expr: UtBoolOpExpression): Int = 19
    override fun visit(expr: UtIsExpression): Int = 20
    override fun visit(expr: UtIteExpression): Int = 21
    override fun visit(expr: UtMkTermArrayExpression): Int = 22
    override fun visit(expr: UtConcatExpression): Int = 23
    override fun visit(expr: UtConvertToString): Int = 24
    override fun visit(expr: UtStringLength): Int = 25
    override fun visit(expr: UtStringPositiveLength): Int = 26
    override fun visit(expr: UtStringCharAt): Int = 27
    override fun visit(expr: UtStringEq): Int = 28
    override fun visit(expr: UtSubstringExpression): Int = 29
    override fun visit(expr: UtReplaceExpression): Int = 30
    override fun visit(expr: UtStartsWithExpression): Int = 31
    override fun visit(expr: UtEndsWithExpression): Int = 32
    override fun visit(expr: UtIndexOfExpression): Int = 33
    override fun visit(expr: UtContainsExpression): Int = 34
    override fun visit(expr: UtToStringExpression): Int = 35
    override fun visit(expr: UtSeqLiteral): Int = 36
    override fun visit(expr: UtConstArrayExpression): Int = 37
    override fun visit(expr: UtGenericExpression): Int = 38
    override fun visit(expr: UtIsGenericTypeExpression): Int = 39
    override fun visit(expr: UtEqGenericTypeParametersExpression): Int = 40
    override fun visit(expr: UtInstanceOfExpression): Int = 41
    override fun visit(expr: UtStringConst): Int = 42
    override fun visit(expr: UtStringToInt): Int = 43
    override fun visit(expr: UtArrayToString): Int = 44
    override fun visit(expr: UtArrayInsert): Int = 45
    override fun visit(expr: UtArrayInsertRange): Int = 46
    override fun visit(expr: UtArrayRemove): Int = 47
    override fun visit(expr: UtArrayRemoveRange): Int = 48
    override fun visit(expr: UtArraySetRange): Int = 49
    override fun visit(expr: UtArrayShiftIndexes): Int = 50
    override fun visit(expr: UtArrayApplyForAll): Int = 51
    override fun visit(expr: UtStringToArray): Int = 52
    override fun visit(expr: UtAddNoOverflowExpression): Int = 53
    override fun visit(expr: UtSubNoOverflowExpression): Int = 54
}


@Suppress("DuplicatedCode")
class UtExpressionGraphExtraction : UtExpressionVisitor<String> {

    private val lineSeparator = System.lineSeparator()
    val expressionIdsCache = IdentityHashMap<UtExpression, String>()
    private val expressionSubGraphCache = IdentityHashMap<UtExpression, String>()
    val literalValues = IdentityHashMap<UtExpression, String>()

    fun getID(expr: UtExpression): String = expressionIdsCache.getOrPut(expr) { UUID.randomUUID().toString() }

    private fun extractSubgraph(expr: UtExpression): String {
        return if (expressionSubGraphCache.containsKey(expr)) {
            ""
        } else {
            val res = expr.accept(this)
            expressionSubGraphCache[expr] = res

            res
        }
    }

    fun extractGraph(input: Iterable<UtExpression>): String {
        return input.fold("FROM,TO$lineSeparator") { acc, expr -> acc + extractSubgraph(expr) }
    }

    override fun visit(expr: UtArraySelectExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression) + extractSubgraph(expr.index)

        res += id + "," + getID(expr.arrayExpression) + lineSeparator
        res += id + "," + getID(expr.index) + lineSeparator

        return res
    }

    override fun visit(expr: UtMkArrayExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        return ""
    }

    override fun visit(expr: UtArrayMultiStoreExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.initial)
        expr.stores.forEach { res += extractSubgraph(it.value) + extractSubgraph(it.index) }

        res += id + "," + getID(expr.initial) + lineSeparator
        expr.stores.forEach { res += id + "," + getID(it.value) + lineSeparator }
        expr.stores.forEach { res += id + "," + getID(it.index) + lineSeparator }

        return res
    }

    override fun visit(expr: UtBvLiteral): String {
        literalValues[expr] = BinaryUtil.binaryValueString(expr.value.toInt())
        return ""
    }

    override fun visit(expr: UtBvConst): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        return ""
    }

    override fun visit(expr: UtAddrExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.internal)
        res += id + "," + getID(expr.internal) + lineSeparator

        return res
    }

    override fun visit(expr: UtFpLiteral): String {
        literalValues[expr] = BinaryUtil.binaryValueString(expr.value.toInt())
        return ""
    }

    override fun visit(expr: UtFpConst): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        return ""
    }

    override fun visit(expr: UtOpExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.left.expr) + extractSubgraph(expr.right.expr)
        res += id + "," + getID(expr.left.expr) + lineSeparator
        res += id + "," + getID(expr.right.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtTrue): String {
        literalValues[expr] = BinaryUtil.binaryValueString(1)
        return ""
    }

    override fun visit(expr: UtFalse): String {
        literalValues[expr] = BinaryUtil.binaryValueString(0)
        return ""
    }

    override fun visit(expr: UtEqExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.left) + extractSubgraph(expr.right)
        res += id + "," + getID(expr.left) + lineSeparator
        res += id + "," + getID(expr.right) + lineSeparator

        return res
    }

    override fun visit(expr: UtBoolConst): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        return ""
    }

    override fun visit(expr: NotBoolExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.expr)
        res += id + "," + getID(expr.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtOrBoolExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = ""
        expr.exprs.forEach { res += extractSubgraph(it) }
        expr.exprs.forEach { res += id + "," + getID(it) + lineSeparator }

        return res
    }

    override fun visit(expr: UtAndBoolExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = ""
        expr.exprs.forEach { res += extractSubgraph(it) }
        expr.exprs.forEach { res += id + "," + getID(it) + lineSeparator }

        return res
    }

    override fun visit(expr: UtNegExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.variable.expr)
        res += id + "," + getID(expr.variable.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtCastExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.variable.expr)
        res += id + "," + getID(expr.variable.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtBoolOpExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.left.expr) + extractSubgraph(expr.right.expr)
        res += id + "," + getID(expr.left.expr) + lineSeparator
        res += id + "," + getID(expr.right.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtIsExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.addr)
        res += id + "," + getID(expr.addr) + lineSeparator

        return res
    }

    override fun visit(expr: UtIteExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.condition) + extractSubgraph(expr.thenExpr) + extractSubgraph(expr.elseExpr)
        res += id + "," + getID(expr.condition) + lineSeparator
        res += id + "," + getID(expr.thenExpr) + lineSeparator
        res += id + "," + getID(expr.elseExpr) + lineSeparator

        return res
    }

    override fun visit(expr: UtConcatExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = ""
        expr.parts.forEach { res += extractSubgraph(it) }
        expr.parts.forEach { res += id + "," + getID(it) + lineSeparator }

        return res
    }

    override fun visit(expr: UtConvertToString): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.expression)
        res += id + "," + getID(expr.expression) + lineSeparator

        return res
    }

    override fun visit(expr: UtStringLength): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string)
        res += id + "," + getID(expr.string) + lineSeparator

        return res
    }

    override fun visit(expr: UtStringPositiveLength): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string)
        res += id + "," + getID(expr.string) + lineSeparator

        return res
    }

    override fun visit(expr: UtStringCharAt): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string) + extractSubgraph(expr.index)
        res += id + "," + getID(expr.string) + lineSeparator
        res += id + "," + getID(expr.index) + lineSeparator

        return res
    }

    override fun visit(expr: UtStringEq): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.left) + extractSubgraph(expr.right)
        res += id + "," + getID(expr.left) + lineSeparator
        res += id + "," + getID(expr.right) + lineSeparator

        return res
    }

    override fun visit(expr: UtSubstringExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string) + extractSubgraph(expr.beginIndex) + extractSubgraph(expr.length)
        res += id + "," + getID(expr.string) + lineSeparator
        res += id + "," + getID(expr.beginIndex) + lineSeparator
        res += id + "," + getID(expr.length) + lineSeparator

        return res
    }

    override fun visit(expr: UtReplaceExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string) + extractSubgraph(expr.regex) + extractSubgraph(expr.replacement)
        res += id + "," + getID(expr.string) + lineSeparator
        res += id + "," + getID(expr.regex) + lineSeparator
        res += id + "," + getID(expr.replacement) + lineSeparator

        return res
    }

    override fun visit(expr: UtStartsWithExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string) + extractSubgraph(expr.prefix)
        res += id + "," + getID(expr.string) + lineSeparator
        res += id + "," + getID(expr.prefix) + lineSeparator

        return res
    }

    override fun visit(expr: UtEndsWithExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string) + extractSubgraph(expr.suffix)
        res += id + "," + getID(expr.string) + lineSeparator
        res += id + "," + getID(expr.suffix) + lineSeparator

        return res
    }

    override fun visit(expr: UtIndexOfExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string) + extractSubgraph(expr.substring)
        res += id + "," + getID(expr.string) + lineSeparator
        res += id + "," + getID(expr.substring) + lineSeparator

        return res
    }

    override fun visit(expr: UtContainsExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.string) + extractSubgraph(expr.substring)
        res += id + "," + getID(expr.string) + lineSeparator
        res += id + "," + getID(expr.substring) + lineSeparator

        return res
    }

    override fun visit(expr: UtToStringExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        val id = getID(expr)
        var res = extractSubgraph(expr.notNullExpr) + extractSubgraph(expr.isNull)
        res += id + "," + getID(expr.notNullExpr) + lineSeparator
        res += id + "," + getID(expr.isNull) + lineSeparator

        return res
    }

    override fun visit(expr: UtSeqLiteral): String {
        literalValues[expr] = BinaryUtil.binaryValueString(expr.value.toInt())
        return ""
    }

    override fun visit(expr: UtMkTermArrayExpression): String {
        literalValues[expr] = BinaryUtil.binaryValueStringEmpty()
        return ""
    }

    override fun visit(expr: UtConstArrayExpression): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.constValue)
        res += id + "," + getID(expr.constValue) + lineSeparator

        return res
    }

    override fun visit(expr: UtGenericExpression): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.addr)
        res += id + "," + getID(expr.addr) + lineSeparator

        return res
    }

    override fun visit(expr: UtIsGenericTypeExpression): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.addr)
        res += extractSubgraph(expr.baseAddr)
        res += id + "," + getID(expr.addr) + lineSeparator
        res += id + "," + getID(expr.baseAddr) + lineSeparator

        return res
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.firstAddr)
        res += extractSubgraph(expr.secondAddr)
        res += id + "," + getID(expr.firstAddr) + lineSeparator
        res += id + "," + getID(expr.secondAddr) + lineSeparator

        return res
    }

    override fun visit(expr: UtInstanceOfExpression): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.constraint)
        res += id + "," + getID(expr.constraint) + lineSeparator

        return res
    }

    override fun visit(expr: UtStringConst): String {
        // TODO
        return ""
    }

    override fun visit(expr: UtStringToInt): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.expression)
        res += id + "," + getID(expr.expression) + lineSeparator

        return res
    }

    override fun visit(expr: UtArrayToString): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression)
        res += extractSubgraph(expr.offset.expr)
        res += extractSubgraph(expr.length.expr)
        res += id + "," + getID(expr.arrayExpression) + lineSeparator
        res += id + "," + getID(expr.offset.expr) + lineSeparator
        res += id + "," + getID(expr.length.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtArrayInsert): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression)
        res += extractSubgraph(expr.element)
        res += extractSubgraph(expr.index.expr)
        res += id + "," + getID(expr.arrayExpression) + lineSeparator
        res += id + "," + getID(expr.element) + lineSeparator
        res += id + "," + getID(expr.index.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtArrayInsertRange): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression)
        res += extractSubgraph(expr.elements)
        res += extractSubgraph(expr.index.expr)
        res += extractSubgraph(expr.from.expr)
        res += extractSubgraph(expr.length.expr)
        res += id + "," + getID(expr.arrayExpression) + lineSeparator
        res += id + "," + getID(expr.elements) + lineSeparator
        res += id + "," + getID(expr.index.expr) + lineSeparator
        res += id + "," + getID(expr.from.expr) + lineSeparator
        res += id + "," + getID(expr.length.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtArrayRemove): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression)
        res += extractSubgraph(expr.index.expr)
        res += id + "," + getID(expr.arrayExpression) + lineSeparator
        res += id + "," + getID(expr.index.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtArrayRemoveRange): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression)
        res += extractSubgraph(expr.index.expr)
        res += extractSubgraph(expr.length.expr)
        res += id + "," + getID(expr.arrayExpression) + lineSeparator
        res += id + "," + getID(expr.index.expr) + lineSeparator
        res += id + "," + getID(expr.length.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtArraySetRange): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression)
        res += extractSubgraph(expr.elements)
        res += extractSubgraph(expr.index.expr)
        res += extractSubgraph(expr.from.expr)
        res += extractSubgraph(expr.length.expr)
        res += id + "," + getID(expr.arrayExpression) + lineSeparator
        res += id + "," + getID(expr.elements) + lineSeparator
        res += id + "," + getID(expr.index.expr) + lineSeparator
        res += id + "," + getID(expr.from.expr) + lineSeparator
        res += id + "," + getID(expr.length.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtArrayShiftIndexes): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression)
        res += extractSubgraph(expr.offset.expr)
        res += id + "," + getID(expr.arrayExpression) + lineSeparator
        res += id + "," + getID(expr.offset.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtArrayApplyForAll): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.arrayExpression)
        res += id + "," + getID(expr.arrayExpression) + lineSeparator

        return res
        // TODO ??
    }

    override fun visit(expr: UtStringToArray): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.stringExpression)
        res += extractSubgraph(expr.offset.expr)
        res += id + "," + getID(expr.stringExpression) + lineSeparator
        res += id + "," + getID(expr.offset.expr) + lineSeparator

        return res
    }

    override fun visit(expr: UtAddNoOverflowExpression): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.left)
        res += extractSubgraph(expr.right)
        res += id + "," + getID(expr.left) + lineSeparator
        res += id + "," + getID(expr.right) + lineSeparator

        return res
    }

    override fun visit(expr: UtSubNoOverflowExpression): String {
        val id = getID(expr)
        var res = extractSubgraph(expr.left)
        res += extractSubgraph(expr.right)
        res += id + "," + getID(expr.left) + lineSeparator
        res += id + "," + getID(expr.right) + lineSeparator

        return res
    }
}
