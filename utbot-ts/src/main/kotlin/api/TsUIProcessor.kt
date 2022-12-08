package api

import parser.TSAstScrapper
import parser.ast.ArrowFunctionNode
import parser.ast.AstNode
import parser.ast.CallExpressionNode
import parser.ast.ClassDeclarationNode
import parser.ast.FunctionNode
import parser.ast.ImportDeclarationNode
import parser.ast.PropertyAccessExpressionNode
import parser.ast.PropertyDeclarationNode
import parser.ast.VariableStatementNode

class TsUIProcessor {

    private data class Context(
        val classes: Set<ClassDeclarationNode> = emptySet(),
        val functions: Set<FunctionNode> = emptySet(),
        val godObject: ClassDeclarationNode
    )

    val staticSet = mutableSetOf<PropertyDeclarationNode>()

    fun traverseCallGraph(rootNode: AstNode, godObject: ClassDeclarationNode) {
        traverse(rootNode, Context(godObject = godObject)) { context ->
            (this as? PropertyAccessExpressionNode)?.let {
                val chain = it.accessChain.value
                val className = chain[0]
                val fieldName = chain[1]
                val clazz = TSAstScrapper(rootNode).findClass(className)
                if (clazz == context.godObject) {
                    val temp = clazz.properties.find { prop -> prop.name == fieldName }
                    if (temp?.isStatic() == true) {
                        staticSet.add(temp)
                    }
                }
            }
        }
    }

    private fun traverse(currentNode: AstNode, context: Context, processor: AstNode.(Context) -> Unit): Context {
        currentNode.processor(context)
        val newContext = currentNode.processNode(context)
        if (currentNode is CallExpressionNode) {
            val functionNode = context.functions.find { it.name.value == currentNode.funcName }
                ?: throw IllegalStateException()
            functionNode.body.fold(newContext) { acc, node ->
                val result = traverse(node, acc, processor)
                Context(
                    setOf(*acc.classes.toTypedArray(), *result.classes.toTypedArray()),
                    setOf(*acc.functions.toTypedArray(), *result.functions.toTypedArray()),
                    godObject = context.godObject
                )
            }
            // We don't want to pass context with function params in it to the call node children.
            currentNode.children.fold(context) { acc, node ->
                val result = traverse(node, acc, processor)
                Context(
                    setOf(*acc.classes.toTypedArray(), *result.classes.toTypedArray()),
                    setOf(*acc.functions.toTypedArray(), *result.functions.toTypedArray()),
                    godObject = context.godObject
                )
            }
        } else {
            currentNode.children.fold(newContext) { acc, node ->
                val result = traverse(node, acc, processor)
                Context(
                    setOf(*acc.classes.toTypedArray(), *result.classes.toTypedArray()),
                    setOf(*acc.functions.toTypedArray(), *result.functions.toTypedArray()),
                    godObject = context.godObject
                )
            }
        }
        return newContext
    }

    // TODO: replace objects in context if a new one has the same name.
    private fun AstNode.processNode(context: Context): Context = when (this) {
        is FunctionNode -> {
            println("I am in function node: $this")
            Context(
                context.classes,
                setOf(this, *context.functions.toTypedArray()),
                godObject = context.godObject
            )
        }

        is ClassDeclarationNode -> {
            println("I am in class node: $this")
            Context(
                setOf(this, *context.classes.toTypedArray()),
                context.functions,
                godObject = context.godObject
            )
        }

        is ImportDeclarationNode -> {
            println("I am in import node: $this")
            val _classes = this.importedNodes.values.filterIsInstance<ClassDeclarationNode>()
            val _functions = this.importedNodes.values.filterIsInstance<FunctionNode>()
            Context(
                setOf(*_classes.toTypedArray(), *context.classes.toTypedArray()),
                setOf(*_functions.toTypedArray(), *context.functions.toTypedArray()),
                godObject = context.godObject
            )
        }

        is CallExpressionNode -> {
            println("I am in call node: $this")
            val lambdaArgs = this.arguments.filterIsInstance<ArrowFunctionNode>().toTypedArray()
            Context(
                context.classes,
                setOf(*lambdaArgs, *context.functions.toTypedArray()),
                godObject = context.godObject
            )
        }

        is VariableStatementNode -> {
            println("I am in variable stm node: $this")
            this.variableDeclarations.fold(context) { acc, node ->
                if (node.value is FunctionNode) {
                    Context(
                        acc.classes,
                        setOf(node.value, *acc.functions.toTypedArray()),
                        godObject = context.godObject
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
}