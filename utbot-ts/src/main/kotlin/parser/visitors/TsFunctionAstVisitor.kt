package parser.visitors

import parser.ast.ClassDeclarationNode
import parser.ast.FunctionDeclarationNode
import parser.ast.FunctionNode
import parser.ast.MethodDeclarationNode

class TsFunctionAstVisitor(
    private val target: String,
    private val className: String?
): AbstractAstVisitor() {

    private var lastVisitedClassName: String = ""
    lateinit var targetFunctionNode: FunctionNode

    override fun visitClassDeclarationNode(node: ClassDeclarationNode): Boolean {
        lastVisitedClassName = node.name
        return true
    }

    override fun visitFunctionDeclarationNode(node: FunctionDeclarationNode): Boolean {
        if (node.name == target && (className ?: "") == lastVisitedClassName) {
            targetFunctionNode = node
            return false
        }
        return true
    }

    override fun visitMethodDeclarationNode(node: MethodDeclarationNode): Boolean {
        if (node.name == target) {
            targetFunctionNode = node
            return false
        }
        return true
    }
}