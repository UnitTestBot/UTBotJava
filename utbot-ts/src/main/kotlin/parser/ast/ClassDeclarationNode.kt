package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren

class ClassDeclarationNode(
    obj: V8Object,
    typescript: V8Object
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(typescript) }

    val name: String = (obj.get("name") as V8Object).getString("escapedText")

    val constructor = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind(typescript) }
        .find { it is ConstructorNode } as ConstructorNode?

    val methods = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind(typescript) }
        .filterIsInstance<MethodDeclarationNode>()

    val properties = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind(typescript) }
        .filterIsInstance<PropertyDeclarationNode>()
}