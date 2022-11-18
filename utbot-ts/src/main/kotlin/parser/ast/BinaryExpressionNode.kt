package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class BinaryExpressionNode(
    obj: V8Object,
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind() }

    val binaryOperator = obj.getObject("operatorToken").getBinaryOperatorNode()

    val leftOperand = obj.getObject("left").getAstNodeByKind()

    val rightOperand = obj.getObject("right").getAstNodeByKind()

}