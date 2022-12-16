package org.utbot.language.ts.parser.ast

abstract class AstNode {

    abstract val children: List<AstNode>

    abstract val parent: AstNode?
}
