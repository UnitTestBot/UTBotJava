package api

import parser.ast.ArrowFunctionNode
import parser.ast.AstNode
import parser.ast.CallExpressionNode
import parser.ast.ClassDeclarationNode
import parser.ast.FunctionNode
import parser.ast.ImportDeclarationNode
import parser.ast.PropertyDeclarationNode
import parser.ast.VariableStatementNode

class TsUIProcessor {

    fun traverseCallGraph(rootNode: AstNode) {
        traverse(rootNode, Context(scope = rootNode))
    }

    private fun traverse(currentNode: AstNode, context: Context): Context {
        val newContext = currentNode.processNode(context)
        if (currentNode is CallExpressionNode) {
            val functionNode = context.functions.find { it.name.value == currentNode.funcName }
                ?: throw IllegalStateException()
            functionNode.body.fold(newContext) { acc, node ->
                val result = traverse(node, acc)
                Context(
                    setOf(*acc.classes.toTypedArray(), *result.classes.toTypedArray()),
                    setOf(*acc.functions.toTypedArray(), *result.functions.toTypedArray()),
                    context.scope
                )
            }
            // We don't want to pass context with function params in it to the call node children.
            currentNode.children.fold(context) { acc, node ->
                val result = traverse(node, acc)
                Context(
                    setOf(*acc.classes.toTypedArray(), *result.classes.toTypedArray()),
                    setOf(*acc.functions.toTypedArray(), *result.functions.toTypedArray()),
                    context.scope
                )
            }
        } else {
            currentNode.children.fold(newContext) { acc, node ->
                val result = traverse(node, acc)
                Context(
                    setOf(*acc.classes.toTypedArray(), *result.classes.toTypedArray()),
                    setOf(*acc.functions.toTypedArray(), *result.functions.toTypedArray()),
                    context.scope
                )
            }
        }
        return newContext
    }
    
    private fun AstNode.processNode(context: Context): Context = when (this) {
        is FunctionNode -> {
            println("I am in function node: $this")
            Context(
                context.classes,
                setOf(this, *context.functions.toTypedArray()),
                context.scope
            )
        }
        is ClassDeclarationNode -> {
            println("I am in class node: $this")
            Context(
                setOf(this, *context.classes.toTypedArray()),
                context.functions,
                context.scope
            )
        }
        is ImportDeclarationNode -> {
            println("I am in import node: $this")
            val _classes = this.importedNodes.values.filterIsInstance<ClassDeclarationNode>()
            val _functions = this.importedNodes.values.filterIsInstance<FunctionNode>()
            Context(
                setOf(*_classes.toTypedArray(), *context.classes.toTypedArray()),
                setOf(*_functions.toTypedArray(), *context.functions.toTypedArray()),
                context.scope
            )
        }
        is CallExpressionNode -> {
            println("I am in call node: $this")
            val lambdaArgs = this.arguments.filterIsInstance<ArrowFunctionNode>().toTypedArray()
            Context(
                context.classes,
                setOf(*lambdaArgs, *context.functions.toTypedArray()),
                context.scope
            )
        }
        is VariableStatementNode -> {
            println("I am in variable stm node: $this")
            this.variableDeclarations.fold(context) { acc, node ->
                if (node.value is FunctionNode) {
                    Context(
                        acc.classes,
                        setOf(node.value, *acc.functions.toTypedArray()),
                        acc.scope
                    )
                } else acc
            }
        }
//        is VariableDeclarationNode -> {
//            if (this.value is FunctionNode) {
//                Context(
//                    context.classes,
//                    setOf(this.value, *context.functions.toTypedArray()),
//                    context.scope
//                )
//            } else context
//        }
        else -> context
    }

    private data class Context(
        val classes: Set<ClassDeclarationNode> = emptySet(),
        val functions: Set<FunctionNode> = emptySet(),
        val scope: AstNode
    )

    fun collectStatics(classNode: ClassDeclarationNode): List<PropertyDeclarationNode> {
        return classNode.properties.filter { it.isStatic() }
    }
}