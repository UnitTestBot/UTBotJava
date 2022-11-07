package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

abstract class FunctionNode(
    obj: V8Object,
    typescript: V8Object
): AstNode() {
    override val children = obj.getChildren().map { it.getAstNodeByKind(typescript) }

    val name: String = (obj.get("name") as V8Object).getString("escapedText")

    @Suppress("UNCHECKED_CAST")
    val parameters = (obj.getArrayAsList("parameters")).map { it.getAstNodeByKind(typescript) }
            as List<ParameterNode>

    val returnType = (obj.get("type") as V8Object).getTypeNode(typescript)
}