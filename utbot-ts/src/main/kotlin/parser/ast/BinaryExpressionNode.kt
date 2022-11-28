package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class BinaryExpressionNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(this) }

    val binaryOperator = obj.getObject("operatorToken").getBinaryOperatorNode(this)

    val leftOperand = obj.getObject("left").getAstNodeByKind(this)

    val rightOperand = obj.getObject("right").getAstNodeByKind(this)

}