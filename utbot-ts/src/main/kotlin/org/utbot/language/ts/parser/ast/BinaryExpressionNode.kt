package org.utbot.language.ts.parser.ast

import com.eclipsesource.v8.V8Object
import org.utbot.language.ts.parser.TsParserUtils.getAstNodeByKind
import org.utbot.language.ts.parser.TsParserUtils.getChildren

class BinaryExpressionNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(this) }

    val binaryOperator = obj.getObject("operatorToken").getBinaryOperatorNode(this)

    val leftOperand = obj.getObject("left").getAstNodeByKind(this)

    val rightOperand = obj.getObject("right").getAstNodeByKind(this)

}