package parser

import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.rhino.Node
import framework.api.js.util.jsBooleanClassId
import framework.api.js.util.jsDoubleClassId
import framework.api.js.util.jsStringClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedContext
import parser.JsParserUtils.getAnyValue
import parser.JsParserUtils.getBinaryExprLeftOperand
import parser.JsParserUtils.getBinaryExprRightOperand
import parser.JsParserUtils.toFuzzedContextComparisonOrNull

class JsFuzzerAstVisitor : IAstVisitor {

    private var lastFuzzedOpGlobal: FuzzedContext = FuzzedContext.Unknown
    val fuzzedConcreteValues = mutableSetOf<FuzzedConcreteValue>()

    override fun accept(rootNode: Node) {
        NodeUtil.visitPreOrder(rootNode) { node ->
            val currentFuzzedOp = node.toFuzzedContextComparisonOrNull()
            when {
                node.isCase -> validateNode(node.firstChild?.getAnyValue())
                currentFuzzedOp != null -> {
                    lastFuzzedOpGlobal = currentFuzzedOp
                    validateNode(node.getBinaryExprLeftOperand().getAnyValue())
                    lastFuzzedOpGlobal = if (lastFuzzedOpGlobal is FuzzedContext.Comparison)
                            (lastFuzzedOpGlobal as FuzzedContext.Comparison).reverse() else FuzzedContext.Unknown
                    validateNode(node.getBinaryExprRightOperand().getAnyValue())
                }
            }
        }

    }

    private fun validateNode(value: Any?) {
        when (value) {
            is String -> {
                fuzzedConcreteValues.add(
                    FuzzedConcreteValue(
                        jsStringClassId,
                        value.toString(),
                        lastFuzzedOpGlobal
                    )
                )
            }

            is Boolean -> {
                fuzzedConcreteValues.add(
                    FuzzedConcreteValue(
                        jsBooleanClassId,
                        value,
                        lastFuzzedOpGlobal
                    )
                )
            }

            is Double -> {
                fuzzedConcreteValues.add(FuzzedConcreteValue(jsDoubleClassId, value, lastFuzzedOpGlobal))
            }
        }
    }
}