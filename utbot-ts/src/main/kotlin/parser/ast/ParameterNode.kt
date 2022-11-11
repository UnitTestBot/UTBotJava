package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class ParameterNode(
    obj: V8Object,
    typescript: V8Object
): AstNode()  {

    override val children = obj.getChildren().map { it.getAstNodeByKind(typescript) }

    val type = (obj.get("type") as V8Object).getTypeNode(typescript)

    val name = (obj.get("name") as V8Object).getString("escapedText")
}