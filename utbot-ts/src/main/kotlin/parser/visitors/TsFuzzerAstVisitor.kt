package parser.visitors

import framework.api.ts.util.tsNumberClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedContext
import parser.ast.AstNode
import parser.ast.BinaryExpressionNode
import parser.ast.ComparisonBinaryOperatorNode
import parser.ast.LiteralNode
import parser.ast.NumericLiteralNode

class TsFuzzerAstVisitor: AbstractAstVisitor() {

    private var lastFuzzedOpGlobal: FuzzedContext = FuzzedContext.Unknown

    val fuzzedConcreteValues = mutableSetOf<FuzzedConcreteValue>()

    override fun visitBinaryExpressionNode(node: BinaryExpressionNode): Boolean {
        val curOp = node.binaryOperator as? ComparisonBinaryOperatorNode ?: return true
        lastFuzzedOpGlobal = curOp.toFuzzedContext()
        validateNode(node.leftOperand)
        lastFuzzedOpGlobal = if (lastFuzzedOpGlobal is FuzzedContext.Comparison) (lastFuzzedOpGlobal as FuzzedContext.Comparison).reverse() else FuzzedContext.Unknown
        validateNode(node.rightOperand)
        return true
    }

    private fun validateNode(literalNode: AstNode) {
        if (literalNode !is LiteralNode) return
        when (literalNode) {
//            is TruffleString -> {
//                fuzzedConcreteValues.add(
//                    FuzzedConcreteValue(
//                        jsStringClassId,
//                        literalNode.value.toString(),
//                        lastFuzzedOpGlobal
//                    )
//                )
//            }
//
//            is Boolean -> {
//                fuzzedConcreteValues.add(
//                    FuzzedConcreteValue(
//                        jsBooleanClassId,
//                        literalNode.value,
//                        lastFuzzedOpGlobal
//                    )
//                )
//            }

            is NumericLiteralNode -> {
                fuzzedConcreteValues.add(FuzzedConcreteValue(tsNumberClassId, literalNode.value, FuzzedContext.Comparison.valueOf("")))
            }
        }
    }
}