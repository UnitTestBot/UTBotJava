package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class PropertyAccessExpressionNode(
    obj: V8Object,
    override val parent: AstNode?
) : AstNode() {

    override val children = obj.getChildren().map { it.getAstNodeByKind(parent) }

    val type = obj.getObject("type").getTypeNode(parent)

    val propertyName: String = obj.getObject("name").getString("escapedText")

    val className: String = obj.getObject("expression").getString("escapedText")
}
