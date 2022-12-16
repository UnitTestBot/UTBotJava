package org.utbot.language.ts.parser.ast

import com.eclipsesource.v8.V8Object
import org.utbot.language.ts.parser.TsParserUtils.getAstNodeByKind
import org.utbot.language.ts.parser.TsParserUtils.getChildren

class ParameterNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode()  {

    override val children = obj.getChildren().map { it.getAstNodeByKind(this) }

    // Implemented not to fail with "keyof typeof" constructions
    // TODO: Research type operators and support them.
    val type = try {
        obj.getObject("type").getTypeNode(this)
    } catch(e: Exception) {
        BaseTypeNode(obj = obj, typeLiteral = "Debug", parent = this)
    }


    val name: String = obj.getObject("name").getString("escapedText")
}