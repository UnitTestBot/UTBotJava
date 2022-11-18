package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class PropertyDeclarationNode(
    obj: V8Object,
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind() }

    val name: String = obj.getObject("name").getString("escapedText")

    val type = obj.getObject("type").getTypeNode()
}