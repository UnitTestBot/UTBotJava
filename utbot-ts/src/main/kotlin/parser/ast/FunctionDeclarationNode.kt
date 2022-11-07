package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class FunctionDeclarationNode(
    obj: V8Object,
    typescript: V8Object
): AstNode() {

    override val children = obj.getChildren().map { it.getAstNodeByKind(typescript) }

    val parameters = (obj.getArrayAsList("parameters")).map { it.getAstNodeByKind(typescript) }

    val returnType = (obj.get("type") as V8Object).getTypeNode(typescript)

    val name: String = (obj.get("name") as V8Object).getString("escapedText")
}