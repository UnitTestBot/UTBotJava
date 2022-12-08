package parser.ast

class EmptyNode: AstNode() {

    override val children: List<AstNode> = emptyList()

    override val parent: AstNode? = null
}