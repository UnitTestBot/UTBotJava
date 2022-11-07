package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class MethodDeclarationNode(
    obj: V8Object,
    typescript: V8Object
): AstNode() {

    override val children = obj.getChildren().map { it.getAstNodeByKind(typescript) }

    val name: String = (obj.get("name") as V8Object).getString("escapedText")

    val parameters = (obj.getArrayAsList("parameters")).map { it.getAstNodeByKind(typescript) }
}