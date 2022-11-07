package parser.ast

import com.eclipsesource.v8.V8Object

class IdentifierNode(
    private val obj: V8Object
): AstNode() {

    override val children: List<AstNode> = emptyList()

    override fun toString(): String = obj.getString("escapedText")
}