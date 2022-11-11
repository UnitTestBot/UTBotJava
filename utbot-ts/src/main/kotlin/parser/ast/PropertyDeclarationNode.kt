package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class PropertyDeclarationNode(
    obj: V8Object,
    typescript: V8Object
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(typescript) }

    val name: String = (obj.get("name") as V8Object).getString("escapedText")

    val type = (obj.get("type") as V8Object).getTypeNode(typescript)
}