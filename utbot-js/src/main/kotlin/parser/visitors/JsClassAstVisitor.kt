package parser.visitors

import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.rhino.Node
import parser.JsParserUtils.getClassName

class JsClassAstVisitor(
    private val target: String?
): IAstVisitor {

    lateinit var targetClassNode: Node
    lateinit var atLeastSomeClassNode: Node
    var classNodesCount = 0

    override fun accept(rootNode: Node) =
        NodeUtil.visitPreOrder(rootNode) {
            if (it.isClass) {
                classNodesCount++
                atLeastSomeClassNode = it
                if (it.getClassName() == target) {
                    targetClassNode = it
                    return@visitPreOrder
                }
            }
        }
}
