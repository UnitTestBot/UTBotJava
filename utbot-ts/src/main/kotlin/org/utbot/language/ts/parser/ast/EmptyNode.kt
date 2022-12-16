package org.utbot.language.ts.parser.ast

class EmptyNode: AstNode() {

    override val children: List<AstNode> = emptyList()

    override val parent: AstNode? = null
}