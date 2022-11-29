package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class VariableStatementNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(this) }

    val variableDeclarations: List<VariableDeclarationNode> = obj.getObject("declarationList")
        .getArrayAsList("declarations").map { it.getAstNodeByKind(this) as VariableDeclarationNode }
}