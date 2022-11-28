package parser.ast

import com.eclipsesource.v8.V8Object
import parser.TsParserUtils.getArrayAsList
import parser.TsParserUtils.getAstNodeByKind
import parser.TsParserUtils.getChildren
import parser.TsParserUtils.getKind

class PropertyDeclarationNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(parent) }

    val name: String = obj.getObject("name").getString("escapedText")

    val type = obj.getObject("type").getTypeNode(parent)

    private val modifiers = obj.getArrayAsList("modifiers").map { it.getKind() }

    fun isStatic() = modifiers.contains("StaticKeyword")
}