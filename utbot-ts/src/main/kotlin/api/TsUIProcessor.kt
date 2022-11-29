package api

import parser.ast.AstNode
import parser.ast.ClassDeclarationNode
import parser.ast.FunctionNode
import parser.ast.ImportDeclarationNode
import parser.ast.PropertyDeclarationNode

class TsUIProcessor {

    fun traverseCallGraph(rootNode: AstNode) {
        buildContext(rootNode)
    }

    private val nodeToContext = mutableMapOf<AstNode, Context>()
    
    private fun buildContext(startNode: AstNode) {
        val nodeQueue = ArrayDeque<AstNode>()
        nodeQueue.add(startNode)
        nodeToContext[startNode] = Context()
        while (nodeQueue.isNotEmpty()) {
            val currentNode = nodeQueue.removeFirst()
            val currentContext = nodeToContext[currentNode] ?: throw IllegalStateException()
            currentNode.children.fold(currentContext) { acc, node ->
                val newContext = node.processNode(acc)
                nodeToContext[node] = newContext
                nodeQueue.add(node)
                newContext
            }
        }
    }
    
    private fun AstNode.processNode(context: Context): Context = when (this) {
        is FunctionNode -> Context(context.classes, setOf(this, *context.functions.toTypedArray()))
        is ClassDeclarationNode -> Context(setOf(this, *context.classes.toTypedArray()), context.functions)
        is ImportDeclarationNode -> {
            val _classes = this.importedNodes.values.filterIsInstance<ClassDeclarationNode>()
            val _functions = this.importedNodes.values.filterIsInstance<FunctionNode>()
            Context(
                setOf(*_classes.toTypedArray(), *context.classes.toTypedArray()),
                setOf(*_functions.toTypedArray(), *context.functions.toTypedArray())
            )
        }
        else -> context
    }

    private data class Context(
        val classes: Set<ClassDeclarationNode> = emptySet(),
        val functions: Set<FunctionNode> = emptySet(),
    )

    fun collectStatics(classNode: ClassDeclarationNode): List<PropertyDeclarationNode> {
        return classNode.properties.filter { it.isStatic() }
    }

}
