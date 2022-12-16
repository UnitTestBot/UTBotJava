package org.utbot.language.ts.parser

import org.utbot.language.ts.parser.ast.AstNode
import org.utbot.language.ts.parser.ast.ClassDeclarationNode
import org.utbot.language.ts.parser.ast.FunctionDeclarationNode
import org.utbot.language.ts.parser.ast.ImportDeclarationNode
import org.utbot.language.ts.parser.ast.MethodDeclarationNode
import org.utbot.language.ts.parser.visitors.AbstractAstVisitor
import org.utbot.language.ts.parser.visitors.TsClassAstVisitor
import org.utbot.language.ts.parser.visitors.TsFunctionAstVisitor

class TSAstScrapper(private val parsedFile: AstNode) {

    fun findFunction(key: String): FunctionDeclarationNode? {
        if (importsMap[key] is FunctionDeclarationNode) return importsMap[key] as FunctionDeclarationNode
        val functionVisitor = TsFunctionAstVisitor(key, null)
        functionVisitor.accept(parsedFile)
        return try {
            functionVisitor.targetFunctionNode as FunctionDeclarationNode
        } catch(e: Exception) { null }

    }

    fun findClass(key: String): ClassDeclarationNode? {
        if (importsMap[key] is ClassDeclarationNode) return importsMap[key] as ClassDeclarationNode
        val classVisitor = TsClassAstVisitor(key)
        classVisitor.accept(parsedFile)
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
        visitor.accept(parsedFile)
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