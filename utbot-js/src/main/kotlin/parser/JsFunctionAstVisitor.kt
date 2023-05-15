package parser

import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.rhino.Node
import parser.JsParserUtils.getAbstractFunctionName
import parser.JsParserUtils.getClassName

class JsFunctionAstVisitor(
    private val target: String,
    private val className: String?
): IAstVisitor {

    lateinit var targetFunctionNode: Node

    override fun accept(rootNode: Node) {
        NodeUtil.visitPreOrder(rootNode) { node ->
            when {
                node.isMemberFunctionDef -> {
                    val name = node.getAbstractFunctionName()
                    if (
                        name == target &&
                        (node.parent?.parent?.getClassName()
                            ?: throw IllegalStateException("Method AST node has no parent class node")) == className
                    ) {
                        targetFunctionNode = node
                        return@visitPreOrder
                    }
                }

                node.isFunction -> {
                    val name = node.getAbstractFunctionName()
                    if (name == target && className == null && node.parent?.isMemberFunctionDef != true) {
                        targetFunctionNode = node
                        return@visitPreOrder
                    }
                }
            }
        }
    }
}
