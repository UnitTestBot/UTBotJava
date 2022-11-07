package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class BinaryExpressionNode(
    obj: V8Object,
    typescript: V8Object
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(typescript) }

    val binaryOperator = (obj.get("operatorToken") as V8Object).getBinaryOperatorNode(typescript)

    val leftOperand = (obj.get("left") as V8Object).getAstNodeByKind(typescript)

    val rightOperand = (obj.get("right") as V8Object).getAstNodeByKind(typescript)

}