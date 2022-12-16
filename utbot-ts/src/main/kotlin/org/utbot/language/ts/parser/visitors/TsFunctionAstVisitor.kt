package org.utbot.language.ts.parser.visitors

import org.utbot.language.ts.parser.ast.ClassDeclarationNode
import org.utbot.language.ts.parser.ast.FunctionDeclarationNode
import org.utbot.language.ts.parser.ast.FunctionNode
import org.utbot.language.ts.parser.ast.MethodDeclarationNode

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
        if (node.name.value == target && className == null) {
            targetFunctionNode = node
            return false
        }
        return true
    }

    override fun visitMethodDeclarationNode(node: MethodDeclarationNode): Boolean {
        if (node.name.value == target && (className ?: "") == lastVisitedClassName) {
            targetFunctionNode = node
            return false
        }
        return true
    }
}