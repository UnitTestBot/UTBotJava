package parser

import parser.ast.AstNode
import parser.ast.ClassDeclarationNode
import parser.ast.FunctionDeclarationNode
import parser.ast.ImportDeclarationNode
import parser.ast.MethodDeclarationNode
import parser.visitors.AbstractAstVisitor
import parser.visitors.TsClassAstVisitor
import parser.visitors.TsFunctionAstVisitor

class TSAstScrapper {

    fun findFunction(key: String): FunctionDeclarationNode? {
        if (importsMap[key] is FunctionDeclarationNode) return importsMap[key] as FunctionDeclarationNode
        val functionVisitor = TsFunctionAstVisitor(key, null)
        return try {
            functionVisitor.targetFunctionNode as FunctionDeclarationNode
        } catch(e: Exception) { null }

    }

    fun findClass(key: String): ClassDeclarationNode? {
        if (importsMap[key] is ClassDeclarationNode) return importsMap[key] as ClassDeclarationNode
        val classVisitor = TsClassAstVisitor(key)
        return try {
            classVisitor.targetClassNode
        } catch (e: Exception) { null }
    }

    fun findMethod(classKey: String, methodKey: String): MethodDeclarationNode? {
        val classNode = findClass(classKey)
        return classNode?.methods?.find { it.name.value == methodKey }
    }

    private val importsMap = run {
        val visitor = Visitor()
        visitor.importNodes.fold(emptyMap<String, AstNode>()) { acc, node ->
            mapOf(*acc.toList().toTypedArray(), *node.importedNodes.toList().toTypedArray())
        }
    }


    private class Visitor: AbstractAstVisitor() {

        private val _importNodes = mutableListOf<ImportDeclarationNode>()

        val importNodes: List<ImportDeclarationNode>
            get() = _importNodes.toList()

        override fun visitImportDeclarationNode(node: ImportDeclarationNode): Boolean {
            _importNodes += node
            return true
        }
    }
}