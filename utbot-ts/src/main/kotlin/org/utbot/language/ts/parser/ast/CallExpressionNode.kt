package org.utbot.language.ts.parser.ast

import com.eclipsesource.v8.V8Object
import org.utbot.language.ts.parser.TsParserUtils.getArrayAsList
import org.utbot.language.ts.parser.TsParserUtils.getAstNodeByKind
import org.utbot.language.ts.parser.TsParserUtils.getChildren

class CallExpressionNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children: List<AstNode> = obj.getChildren().map { it.getAstNodeByKind(this) }

    val arguments = obj.getArrayAsList("arguments").map { it.getAstNodeByKind(this) }

    val funcName: String = obj.getObject("expression").getString("escapedText")
}

