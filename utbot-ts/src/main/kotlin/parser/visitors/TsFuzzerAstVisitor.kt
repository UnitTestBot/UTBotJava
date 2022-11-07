package parser.visitors

import framework.api.ts.util.tsNumberClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedContext
import parser.ast.AstNode
import parser.ast.ComparisonBinaryOperatorNode
import parser.ast.LiteralNode
import parser.ast.NumericLiteralNode

class TsFuzzerAstVisitor: AbstractAstVisitor() {

    private var lastFuzzedOpGlobal: FuzzedContext = FuzzedContext.Unknown

    val fuzzedConcreteValues = mutableSetOf<FuzzedConcreteValue>()

    override fun visitComparisonBinaryOperationNode(node: ComparisonBinaryOperatorNode): Boolean {
        val compOp = """>=|<=|>|<|==|!=""".toRegex()
        val curOp = compOp.find(node.toString())?.value
        val currentFuzzedOp = FuzzedContext.Comparison.values().find { curOp == it.sign } ?: FuzzedContext.Unknown
        lastFuzzedOpGlobal = currentFuzzedOp
        validateNode(node.left)
        lastFuzzedOpGlobal = if (lastFuzzedOpGlobal is FuzzedContext.Comparison) (lastFuzzedOpGlobal as FuzzedContext.Comparison).reverse() else FuzzedContext.Unknown
        validateNode(node.right)
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
                fuzzedConcreteValues.add(FuzzedConcreteValue(tsNumberClassId, literalNode.value, lastFuzzedOpGlobal))
            }
        }
    }
}