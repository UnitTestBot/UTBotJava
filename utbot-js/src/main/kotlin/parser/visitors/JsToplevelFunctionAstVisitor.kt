package parser.visitors

import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.rhino.Node

class JsToplevelFunctionAstVisitor : IAstVisitor {

    val extractedMethods = mutableListOf<Node>()


    override fun accept(rootNode: Node) {
        NodeUtil.visitPreOrder(rootNode) { node ->
            when {
                node.isFunction && !(node.parent?.isMemberFunctionDef ?: true) -> extractedMethods += node
            }
        }
    }
}
