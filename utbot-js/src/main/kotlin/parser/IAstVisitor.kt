package parser

import com.google.javascript.rhino.Node

interface IAstVisitor {

    fun accept(rootNode: Node)
}