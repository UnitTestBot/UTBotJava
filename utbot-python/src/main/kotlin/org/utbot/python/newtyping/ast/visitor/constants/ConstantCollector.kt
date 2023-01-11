package org.utbot.python.newtyping.ast.visitor.constants

import org.parsers.python.Node
import org.parsers.python.PythonConstants
import org.parsers.python.ast.NumericalLiteral
import org.parsers.python.ast.StringLiteral
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.ast.typeOfNumericalLiteral
import org.utbot.python.newtyping.ast.visitor.Collector
import org.utbot.python.newtyping.general.Type

class ConstantCollector(private val storage: PythonTypeStorage): Collector() {
    val result = mutableListOf<Pair<Type, Any>>()

    override fun collectFromNodeAfterRecursion(node: Node) {
        when (node) {
            is NumericalLiteral -> processNumericalLiteral(node)
            is StringLiteral -> processStringLiteral(node)
        }
    }

    private fun processNumericalLiteral(node: NumericalLiteral) {
        when (val type = typeOfNumericalLiteral(node, storage)) {
            storage.pythonInt -> {
                if (node.type == PythonConstants.TokenType.DECNUMBER)
                    result.add(Pair(type, node.toString().toBigInteger()))
            }
            storage.pythonFloat -> result.add(Pair(type, node.toString().toBigDecimal()))
        }
    }

    private fun processStringLiteral(node: StringLiteral) {
        result.add(Pair(storage.pythonStr, node.toString()))
    }
}