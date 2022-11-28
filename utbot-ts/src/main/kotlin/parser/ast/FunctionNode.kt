package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

abstract class FunctionNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {
    override val children = obj.getChildren().map { it.getAstNodeByKind(this) }

    val name: String = obj.getObject("name").getString("escapedText")

    @Suppress("UNCHECKED_CAST")
    val parameters = (obj.getArrayAsList("parameters")).map { it.getAstNodeByKind(this) }
            as List<ParameterNode>

    val returnType = obj.getObject("type").getTypeNode(this)
}