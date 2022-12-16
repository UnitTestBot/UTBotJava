package org.utbot.language.ts.parser.ast

import com.eclipsesource.v8.V8Object
import org.utbot.language.ts.parser.TsParserUtils.getAstNodeByKind
import org.utbot.language.ts.parser.TsParserUtils.getChildren

class DummyNode(
    obj: V8Object,
    override val parent: AstNode?
): AstNode() {

    override val children = obj.getChildren().map { it.getAstNodeByKind(this) }
}