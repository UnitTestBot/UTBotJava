package parser.visitors

import parser.ast.ClassDeclarationNode
import parser.ast.FunctionDeclarationNode
import parser.ast.FunctionNode

class TsToplevelFunctionAstVisitor: AbstractAstVisitor() {

    val extractedMethods = mutableListOf<FunctionNode>()

    override fun visitClassDeclarationNode(node: ClassDeclarationNode): Boolean {
        return false
    }

    override fun visitFunctionDeclarationNode(node: FunctionDeclarationNode): Boolean {
        extractedMethods += node
        return false
    }
}