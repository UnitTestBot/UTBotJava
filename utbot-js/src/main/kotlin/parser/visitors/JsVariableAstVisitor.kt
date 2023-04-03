package parser.visitors

import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.rhino.Node
import parser.JsParserUtils.getVariableName
import parser.JsParserUtils.isAnyVariableDecl

class JsVariableAstVisitor(
    private val target: String
): IAstVisitor {

    lateinit var targetVariableNode: Node

    override fun accept(rootNode: Node) =
        NodeUtil.visitPreOrder(rootNode) {
            if (it.isAnyVariableDecl() && it.getVariableName() == target) {
                targetVariableNode = it
                return@visitPreOrder
            }
        }
}
