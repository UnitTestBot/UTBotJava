package parser.visitors

import parser.ast.ClassDeclarationNode
import parser.ast.FunctionDeclarationNode

class TsToplevelFunctionAstVisitor: AbstractAstVisitor() {

    val extractedMethods = mutableListOf<FunctionDeclarationNode>()

    override fun visitClassDeclarationNode(node: ClassDeclarationNode): Boolean {
        return false
    }

    override fun visitFunctionDeclarationNode(node: FunctionDeclarationNode): Boolean {
        extractedMethods += node
        return false
    }
}