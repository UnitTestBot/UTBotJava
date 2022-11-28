package parser.ast

import com.eclipsesource.v8.V8Object
import com.eclipsesource.v8.utils.V8ObjectUtils
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class CallExpressionNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(this) }

    val arguments = obj.getArrayAsList("arguments").map { it.getAstNodeByKind(this) }



    val funcName: String = obj.getObject("expression").getString("escapedText")

    private fun findFuncDeclarationNode(): AstNode {

    }
}

