package parser


import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.rhino.Node
import framework.api.js.util.jsBooleanClassId
import framework.api.js.util.jsDoubleClassId
import framework.api.js.util.jsStringClassId
import fuzzer.JsFuzzedConcreteValue
import fuzzer.JsFuzzedContext
import parser.JsParserUtils.getAnyValue
import parser.JsParserUtils.getBinaryExprLeftOperand
import parser.JsParserUtils.getBinaryExprRightOperand
import parser.JsParserUtils.toFuzzedContextComparisonOrNull

class JsFuzzerAstVisitor : IAstVisitor {

    private var lastFuzzedOpGlobal: JsFuzzedContext = JsFuzzedContext.Unknown
    val fuzzedConcreteValues = mutableSetOf<JsFuzzedConcreteValue>()

    override fun accept(rootNode: Node) {
        NodeUtil.visitPreOrder(rootNode) { node ->
            val currentFuzzedOp = node.toFuzzedContextComparisonOrNull()
            when {
                node.isCase -> validateNode(node.firstChild?.getAnyValue())
                currentFuzzedOp != null -> {
                    lastFuzzedOpGlobal = currentFuzzedOp
                    validateNode(node.getBinaryExprLeftOperand().getAnyValue())
                    lastFuzzedOpGlobal = lastFuzzedOpGlobal.reverse()
                    validateNode(node.getBinaryExprRightOperand().getAnyValue())
                }
            }
        }

    }

    private fun validateNode(value: Any?) {
        when (value) {
            is String -> {
                fuzzedConcreteValues.add(
                    JsFuzzedConcreteValue(
                        jsStringClassId,
                        value.toString(),
                        lastFuzzedOpGlobal
                    )
                )
            }

            is Boolean -> {
                fuzzedConcreteValues.add(
                    JsFuzzedConcreteValue(
                        jsBooleanClassId,
                        value,
                        lastFuzzedOpGlobal
                    )
                )
            }

            is Double -> {
                fuzzedConcreteValues.add(JsFuzzedConcreteValue(jsDoubleClassId, value, lastFuzzedOpGlobal))
            }
        }
    }
}
