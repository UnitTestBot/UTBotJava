package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class ClassDeclarationNode(
    obj: V8Object,
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind() }

    val name: String = obj.getObject("name").getString("escapedText")

    val constructor = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind() }
        .find { it is ConstructorNode } as ConstructorNode?

    val methods = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind() }
        .filterIsInstance<MethodDeclarationNode>()

    val properties = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind() }
        .filterIsInstance<PropertyDeclarationNode>()
}