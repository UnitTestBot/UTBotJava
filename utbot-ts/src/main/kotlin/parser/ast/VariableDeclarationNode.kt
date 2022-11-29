package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind

class VariableDeclarationNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = emptyList()

    val name: String = obj.getObject("name").getString("escapedText")

    val value: AstNode = obj.getObject("initializer").getAstNodeByKind(this)
}