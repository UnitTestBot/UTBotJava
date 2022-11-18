package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class ConstructorNode(
    obj: V8Object,
): AstNode() {

    override val children = obj.getChildren().map { it.getAstNodeByKind() }

    @Suppress("UNCHECKED_CAST")
    val parameters = (obj.getArrayAsList("parameters")).map { it.getAstNodeByKind() }
                        as List<ParameterNode>


}