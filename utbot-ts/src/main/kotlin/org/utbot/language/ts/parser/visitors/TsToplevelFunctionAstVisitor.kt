package org.utbot.language.ts.parser.visitors

import org.utbot.language.ts.parser.ast.ClassDeclarationNode
import org.utbot.language.ts.parser.ast.FunctionDeclarationNode
import org.utbot.language.ts.parser.ast.FunctionNode

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