package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind

class FunctionTypeNode(
    obj: V8Object,
    override val parent: AstNode?
): TypeNode() {

    override val stringTypeName: String = "FunctionType"

    @Suppress("UNCHECKED_CAST")
    val parameters = obj.getArrayAsList("parameters").map { it.getAstNodeByKind(this) }
            as List<ParameterNode>

    val returnType = obj.getObject("type").getTypeNode(this)
}