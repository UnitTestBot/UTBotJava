package parser

import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.jscomp.SourceFile
import com.google.javascript.rhino.Node
import java.io.File
import parser.JsParserUtils.getAbstractFunctionName
import parser.JsParserUtils.getClassMethods
import parser.JsParserUtils.isRequireImport

class JsAstScrapper(private val parsedFile: Node) {

    // Used not to parse the same file multiple times.
    private val _parsedFilesCache = mutableMapOf<String, Node>()

    fun findFunction(key: String): Node? {
        if (importsMap[key]?.isFunction == true) return importsMap[key]
        val functionVisitor = JsFunctionAstVisitor(key, null)
        functionVisitor.accept(parsedFile)
        return try {
            functionVisitor.targetFunctionNode
        } catch(e: Exception) { null }
    }

    fun findClass(key: String): Node? {
        if (importsMap[key]?.isClass == true) return importsMap[key]
        val classVisitor = JsClassAstVisitor(key)
        classVisitor.accept(parsedFile)
        return try {
            classVisitor.targetClassNode
        } catch (e: Exception) { null }
    }

    fun findMethod(classKey: String, methodKey: String): Node? {
        val classNode = findClass(classKey)
        return classNode?.getClassMethods()?.find { it.getAbstractFunctionName() == methodKey }
    }

    private val importsMap = run {
        val visitor = Visitor()
        visitor.accept(parsedFile)
        visitor.importNodes.fold(emptyMap<String, Node>()) { acc, node ->
            mapOf(*acc.toList().toTypedArray(), *node.importedNodes().toList().toTypedArray())
        }
    }

    private fun Node.importedNodes(): Map<String, Node> {
        return when {
            this.isRequireImport() -> mapOf(
                this.parent!!.string to this.firstChild!!.next!!
            )
            else -> emptyMap()
        }
    }

    private fun makePathFromImport() {
//        val relPath = node.importText + if (node.importText.endsWith(".ts")) "" else ".ts"
//        // If import text doesn't contain "/", then it is NodeJS stdlib import.
//        if (!relPath.contains("/")) return true
//        val finalPath = Paths.get(File(basePath).parent).resolve(Paths.get(relPath))
    }

    private fun File.findEntityInFile(key: String): Node? {
        val parsedFile = _parsedFilesCache.putIfAbsent(
            path,
            Compiler().parse(SourceFile.fromCode("jsFile", readText()))
        )!!
        val localScrapper = JsAstScrapper(parsedFile)
        return localScrapper.findClass(key)
            ?: localScrapper.findFunction(key)

    }



    private class Visitor: IAstVisitor {

        private val _importNodes = mutableListOf<Node>()

        val importNodes: List<Node>
            get() = _importNodes.toList()

        override fun accept(rootNode: Node) {
            NodeUtil.visitPreOrder(rootNode) { node ->
                if (node.isImport || node.isRequireImport()) _importNodes += node
            }
        }
    }
}