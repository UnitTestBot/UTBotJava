package org.utbot.python.newtyping.ast.visitor.constants

import org.parsers.python.Node
import org.parsers.python.PythonConstants
import org.parsers.python.ast.NumericalLiteral
import org.parsers.python.ast.SliceExpression
import org.parsers.python.ast.StringLiteral
import org.parsers.python.ast.UnaryExpression
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.ast.SimpleSlice
import org.utbot.python.newtyping.ast.TupleSlice
import org.utbot.python.newtyping.ast.parseSliceExpression
import org.utbot.python.newtyping.ast.typeOfNumericalLiteral
import org.utbot.python.newtyping.ast.visitor.Collector
import org.utbot.python.newtyping.createPythonTupleType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.typesAreEqual
import java.math.BigInteger

class ConstantCollector(private val storage: PythonTypeStorage) : Collector() {
    private val knownConstants = mutableMapOf<Pair<Int, Int>, Pair<Type, Any>>()

    private fun add(node: Node, type: Type, value: Any) {
        knownConstants[Pair(node.beginOffset, node.endOffset)] = Pair(type, value)
    }

    private fun get(node: Node): Pair<Type, Any>? {
        return knownConstants[Pair(node.beginOffset, node.endOffset)]
    }

    val result: List<Pair<Type, Any>>
        get() = knownConstants.map { it.value }

    override fun collectFromNodeAfterRecursion(node: Node) {
        when (node) {
            is NumericalLiteral -> processNumericalLiteral(node)
            is StringLiteral -> processStringLiteral(node)
            is SliceExpression -> processSliceExpression(node)
            is UnaryExpression -> processUnaryExpression(node)
        }
    }

    private fun processNumericalLiteral(node: NumericalLiteral) {
        val type = typeOfNumericalLiteral(node, storage)
        val value = when (type) {
            storage.pythonInt -> {
                if (node.type == PythonConstants.TokenType.DECNUMBER)
                    node.toString().toBigInteger()
                else return
            }
            storage.pythonFloat -> node.toString().toDouble()
            else -> return
        }
        add(node, type, value)
    }

    private fun processStringLiteral(node: StringLiteral) {
        if (node.toString().first() != 'f')
            add(node, storage.pythonStr, node.toString())
    }

    private fun processSliceExpression(node: SliceExpression) {
        val parsed = parseSliceExpression(node) ?: return
        if (parsed.slices !is TupleSlice)
            return
        val elemNodes = parsed.slices.elems.map { it as? SimpleSlice ?: return }
        val elems = elemNodes.map { get(it.indexValue) ?: return }
        val type = createPythonTupleType(elems.map { it.first })
        knownConstants[Pair(elemNodes.first().indexValue.beginOffset, elemNodes.last().indexValue.endOffset)] =
            Pair(type, elems)
    }

    private fun processUnaryExpression(node: UnaryExpression) {
        if (node.children().size != 2 || node.children().first().toString() != "-")
            return
        val child = get(node.children()[1]) ?: return
        val value = child.second
        if (value is BigInteger) {
            add(node, storage.pythonInt, -value)
        }
        if (value is Double) {
            add(node, storage.pythonFloat, -value)
        }
    }
}