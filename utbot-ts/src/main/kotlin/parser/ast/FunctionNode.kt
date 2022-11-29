package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

abstract class FunctionNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    abstract val name: String

    override val children = obj.getChildren().map { it.getAstNodeByKind(this) }

    @Suppress("UNCHECKED_CAST")
    val parameters = obj.getArrayAsList("parameters").map { it.getAstNodeByKind(this) }
            as List<ParameterNode>

    val body = obj.getObject("body").getArrayAsList("statements")
        .map { it.getAstNodeByKind(this) }

   open val returnType = obj.getObject("type").getTypeNode(this)
}