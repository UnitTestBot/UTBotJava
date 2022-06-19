package org.utbot.predictors

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
import org.utbot.features.UtExpressionId
import org.utbot.utils.layers.MessageAggregation
import org.utbot.utils.layers.MessagePassing
import org.utbot.utils.BinaryUtil
import java.io.FileNotFoundException
import java.util.ArrayList
import java.util.HashMap
import java.util.IdentityHashMap
import org.utbot.utils.MatrixUtil


class SimpleDecoder {

    lateinit var weight0: Array<DoubleArray>
    lateinit var weight1: Array<DoubleArray>
    lateinit var weight2: Array<DoubleArray>
    lateinit var weight3: Array<DoubleArray>

    init {
        try {
            weight0 = MatrixUtil.loadMatrix("logs/model.decoder.nn.0.weight", false)
            weight1 = MatrixUtil.loadMatrix("logs/model.decoder.nn.2.weight", false)
            weight2 = MatrixUtil.loadMatrix("logs/model.decoder.nn.4.weight", false)
            weight3 = MatrixUtil.loadMatrix("logs/model.decoder.out.0.weight", false)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    fun propagate(state: DoubleArray): DoubleArray {
        var x: DoubleArray = MatrixUtil.mmul(state, weight0)
        for (i in x.indices) {
            x[i] = Math.max(x[i], 0.0)
        }
        x = MatrixUtil.mmul(x, weight1)
        for (i in x.indices) {
            x[i] = Math.max(x[i], 0.0)
        }
        x = MatrixUtil.mmul(x, weight2)
        for (i in x.indices) {
            x[i] = Math.max(x[i], 0.0)
        }
        x = MatrixUtil.mmul(x, weight3)

        return x
    }
}


@Suppress("DuplicatedCode")
class UtExpressionEncoder : UtExpressionVisitor<DoubleArray> {

    var vertexCounter = 0
    private val states = IdentityHashMap<UtExpression, DoubleArray>()

    companion object {
        private val messageAggregation = MessageAggregation()
        private val messagePassing = MessagePassing()
        private val decoder = SimpleDecoder()
        private val initState: MutableMap<String, DoubleArray> = HashMap()
    }

    fun encode(input: Iterable<UtExpression>): DoubleArray {
        val res = DoubleArray(39)
        var size = 0

        for (expr in input) {
            size += 1
            val enc = encodeNode(expr)
            var i = 0
            while (i < enc.size) {
                res[i] += enc[i]
                i += 1
            }
        }

        return decoder.propagate(res.map { it / size }.toDoubleArray())
    }

    private fun encodeNode(expr: UtExpression): DoubleArray {
        if (!initState.containsKey(expr.javaClass.simpleName)) {
            initState[expr.javaClass.simpleName] = BinaryUtil.binaryExpression(UtExpressionId().getID(expr))
        }
        if (!states.containsKey(expr)) {
            vertexCounter += 1
            states[expr] = expr.accept(this)
        }
        return states[expr]!!
    }

    override fun visit(expr: UtArraySelectExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.index))
        messages.add(encodeNode(expr.arrayExpression))

        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtMkArrayExpression): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty())
    }

    override fun visit(expr: UtArrayMultiStoreExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        for ((index) in expr.stores) {
            messages.add(encodeNode(index))
        }
        messages.add(encodeNode(expr.initial))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtBvLiteral): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValue(expr.value.toInt()))
    }

    override fun visit(expr: UtBvConst): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty())
    }

    override fun visit(expr: UtAddrExpression): DoubleArray {
        val message = encodeNode(expr.internal)
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(message)
        )
    }

    override fun visit(expr: UtFpLiteral): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValue(expr.value.toInt()))
    }

    override fun visit(expr: UtFpConst): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty())
    }

    override fun visit(expr: UtOpExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.left.expr))
        messages.add(encodeNode(expr.right.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtTrue): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValue(1))
    }

    override fun visit(expr: UtFalse): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValue(0))
    }

    override fun visit(expr: UtEqExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.left))
        messages.add(encodeNode(expr.right))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtBoolConst): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty())
    }

    override fun visit(expr: NotBoolExpression): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.expr))
        )
    }

    override fun visit(expr: UtOrBoolExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        for (exp in expr.exprs) {
            messages.add(encodeNode(exp))
        }
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtAndBoolExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        for (exp in expr.exprs) {
            messages.add(encodeNode(exp))
        }
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtNegExpression): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.variable.expr))
        )
    }

    override fun visit(expr: UtCastExpression): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.variable.expr))
        )
    }

    override fun visit(expr: UtBoolOpExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.left.expr))
        messages.add(encodeNode(expr.right.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtIsExpression): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.addr))
        )
    }

    override fun visit(expr: UtIteExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.elseExpr))
        messages.add(encodeNode(expr.thenExpr))
        messages.add(encodeNode(expr.condition))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtConcatExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        for (exp in expr.parts) {
            messages.add(encodeNode(exp))
        }
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtConvertToString): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.expression))
        )
    }

    override fun visit(expr: UtStringLength): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.string))
        )
    }

    override fun visit(expr: UtStringPositiveLength): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.string))
        )
    }

    override fun visit(expr: UtStringCharAt): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.string))
        messages.add(encodeNode(expr.index))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtStringEq): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.left))
        messages.add(encodeNode(expr.right))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtSubstringExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.string))
        messages.add(encodeNode(expr.beginIndex))
        messages.add(encodeNode(expr.length))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtReplaceExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.string))
        messages.add(encodeNode(expr.regex))
        messages.add(encodeNode(expr.replacement))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtStartsWithExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.string))
        messages.add(encodeNode(expr.prefix))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtEndsWithExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.string))
        messages.add(encodeNode(expr.suffix))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtIndexOfExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.string))
        messages.add(encodeNode(expr.substring))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtContainsExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.string))
        messages.add(encodeNode(expr.substring))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtToStringExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.isNull))
        messages.add(encodeNode(expr.notNullExpr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtSeqLiteral): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty())
    }

    override fun visit(expr: UtMkTermArrayExpression): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty())
    }

    override fun visit(expr: UtConstArrayExpression): DoubleArray {
        val message = encodeNode(expr.constValue)
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(message)
        )
    }

    override fun visit(expr: UtGenericExpression): DoubleArray {
        val message = encodeNode(expr.addr)
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(message)
        )
    }

    override fun visit(expr: UtIsGenericTypeExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.addr))
        messages.add(encodeNode(expr.baseAddr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.firstAddr))
        messages.add(encodeNode(expr.secondAddr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtInstanceOfExpression): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.constraint))
        )
    }

    override fun visit(expr: UtStringConst): DoubleArray {
        return concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty())
    }

    override fun visit(expr: UtStringToInt): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.expression))
        )
    }

    override fun visit(expr: UtArrayToString): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.arrayExpression))
        messages.add(encodeNode(expr.offset.expr))
        messages.add(encodeNode(expr.length.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtArrayInsert): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.arrayExpression))
        messages.add(encodeNode(expr.element))
        messages.add(encodeNode(expr.index.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtArrayInsertRange): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.arrayExpression))
        messages.add(encodeNode(expr.elements))
        messages.add(encodeNode(expr.index.expr))
        messages.add(encodeNode(expr.from.expr))
        messages.add(encodeNode(expr.length.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtArrayRemove): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.arrayExpression))
        messages.add(encodeNode(expr.index.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtArrayRemoveRange): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.arrayExpression))
        messages.add(encodeNode(expr.index.expr))
        messages.add(encodeNode(expr.length.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtArraySetRange): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.arrayExpression))
        messages.add(encodeNode(expr.elements))
        messages.add(encodeNode(expr.index.expr))
        messages.add(encodeNode(expr.from.expr))
        messages.add(encodeNode(expr.length.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtArrayShiftIndexes): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.arrayExpression))
        messages.add(encodeNode(expr.offset.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtArrayApplyForAll): DoubleArray {
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(encodeNode(expr.arrayExpression))
        )
    }

    override fun visit(expr: UtStringToArray): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.stringExpression))
        messages.add(encodeNode(expr.offset.expr))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    private fun concat(first: DoubleArray, second: DoubleArray): DoubleArray {
        val res = DoubleArray(first.size + second.size)
        System.arraycopy(first, 0, res, 0, first.size)
        System.arraycopy(second, 0, res, first.size, second.size)
        return res
    }

    override fun visit(expr: UtAddNoOverflowExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.left))
        messages.add(encodeNode(expr.right))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }

    override fun visit(expr: UtSubNoOverflowExpression): DoubleArray {
        val messages = ArrayList<DoubleArray>()
        messages.add(encodeNode(expr.left))
        messages.add(encodeNode(expr.right))
        return messageAggregation.propagate(
            concat(initState[expr.javaClass.simpleName]!!, BinaryUtil.binaryValueEmpty()),
            messagePassing.propagate(messages)
        )
    }
}