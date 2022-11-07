package parser.ast

abstract class LiteralNode: AstNode() {

    override val children: List<AstNode> = emptyList()
    abstract val value: Any
}