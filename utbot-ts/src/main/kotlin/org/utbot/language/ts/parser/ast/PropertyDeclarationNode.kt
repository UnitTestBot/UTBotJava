package org.utbot.language.ts.parser.ast

import com.eclipsesource.v8.V8Object
import org.utbot.language.ts.parser.TsParserUtils.getArrayAsList
import org.utbot.language.ts.parser.TsParserUtils.getAstNodeByKind
import org.utbot.language.ts.parser.TsParserUtils.getChildren
import org.utbot.language.ts.parser.TsParserUtils.getKind

class PropertyDeclarationNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(parent) }

    val name: String = obj.getObject("name").getString("escapedText")

    val type = try {
        obj.getObject("type").getTypeNode(this)
    } catch(e: Exception) {
        BaseTypeNode(obj = obj, typeLiteral = "Debug", parent = this)
    }

    val parentClass = lazy { (parent as? ClassDeclarationNode) ?: throw UnsupportedOperationException() }

    private val modifiers = obj.getArrayAsList("modifiers").map { it.getKind() }

    fun isStatic() = modifiers.contains("StaticKeyword")

}