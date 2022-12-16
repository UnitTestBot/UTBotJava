package org.utbot.language.ts.parser.ast

import com.eclipsesource.v8.V8Object
import org.utbot.language.ts.parser.TsParserUtils.getArrayAsList
import org.utbot.language.ts.parser.TsParserUtils.getAstNodeByKind
import org.utbot.language.ts.parser.TsParserUtils.getChildren

class ClassDeclarationNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(this) }

    val name: String = obj.getObject("name").getString("escapedText")

    val constructor = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind(this) }
        .find { it is ConstructorNode } as ConstructorNode?

    val methods = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind(this) }
        .filterIsInstance<MethodDeclarationNode>()

    val properties = obj.getArrayAsList("members")
        .map { it.getAstNodeByKind(this) }
        .filterIsInstance<PropertyDeclarationNode>()
}