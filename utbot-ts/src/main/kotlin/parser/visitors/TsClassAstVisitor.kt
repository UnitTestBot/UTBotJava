package parser.visitors

import parser.ast.ClassDeclarationNode

class TsClassAstVisitor(private val target: String?): AbstractAstVisitor() {

    lateinit var targetClassNode: ClassDeclarationNode
    lateinit var atLeastSomeClassNode: ClassDeclarationNode
    var classNodesCount = 0

    override fun visitClassDeclarationNode(node: ClassDeclarationNode): Boolean {
        classNodesCount++
        atLeastSomeClassNode = node
        if (node.name == target) {
            targetClassNode = node
            return false
        }
        return true
    }
}