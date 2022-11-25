package api

import parser.ast.AstNode
import parser.ast.ClassDeclarationNode
import parser.ast.FunctionDeclarationNode
import parser.ast.PropertyDeclarationNode

class TsUIProcessor {

    fun traverseCallGraph(node: AstNode) {
        node.children.forEach {
            traverseCallGraph(it)
        }
    }

    fun collectStatics(classNode: ClassDeclarationNode): List<PropertyDeclarationNode> {
        return classNode.properties.filter { it.isStatic() }
    }

}