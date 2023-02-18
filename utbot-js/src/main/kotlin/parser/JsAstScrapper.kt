package parser

import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.jscomp.SourceFile
import com.google.javascript.rhino.Node
import java.io.File
import java.nio.file.Paths
import mu.KotlinLogging
import parser.JsParserUtils.getAbstractFunctionName
import parser.JsParserUtils.getClassMethods
import parser.JsParserUtils.getImportSpecAliases
import parser.JsParserUtils.getImportSpecName
import parser.JsParserUtils.getModuleImportSpecsAsList
import parser.JsParserUtils.getModuleImportText
import parser.JsParserUtils.getRequireImportText
import parser.JsParserUtils.isRequireImport
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

class JsAstScrapper(
    private val parsedFile: Node,
    private val basePath: String,
) {

    // Used not to parse the same file multiple times.
    private val _parsedFilesCache = mutableMapOf<String, Node>()

    fun findFunction(key: String, file: Node): Node? {
        if (importsMap[key]?.isFunction == true) return importsMap[key]
        val functionVisitor = JsFunctionAstVisitor(key, null)
        functionVisitor.accept(file)
        return try {
            functionVisitor.targetFunctionNode
        } catch (e: Exception) { null }
    }

    fun findClass(key: String, file: Node): Node? {
        if (importsMap[key]?.isClass == true) return importsMap[key]
        val classVisitor = JsClassAstVisitor(key)
        classVisitor.accept(file)
        return try {
            classVisitor.targetClassNode
        } catch (e: Exception) { null }
    }

    fun findMethod(classKey: String, methodKey: String, file: Node): Node? {
        val classNode = findClass(classKey, file)
        return classNode?.getClassMethods()?.find { it.getAbstractFunctionName() == methodKey }
    }

    private val importsMap = run {
        val visitor = Visitor()
        visitor.accept(parsedFile)
        visitor.importNodes.fold(emptyMap<String, Node>()) { acc, node ->
            mapOf(*acc.toList().toTypedArray(), *node.importedNodes().toList().toTypedArray())
        }
    }

    private fun File.parseIfNecessary(): Node =
        _parsedFilesCache.getOrPut(this.path) {
            Compiler().parse(SourceFile.fromCode(this.path, readText()))
        }

    private fun Node.importedNodes(): Map<String, Node> {
        return when {
            this.isRequireImport() -> mapOf(
                this.parent!!.string to (makePathFromImport(this.getRequireImportText())?.let {
                    File(it).parseIfNecessary().findEntityInFile(null)
                    // Workaround for std imports.
                } ?: this.firstChild!!.next!!)
            )
            this.isImport -> this.processModuleImport()
            else -> emptyMap()
        }
    }

    private fun Node.processModuleImport(): Map<String, Node> {
        try {
            val pathToFile = makePathFromImport(this.getModuleImportText()) ?: return emptyMap()
            val pFile = File(pathToFile).parseIfNecessary()
            return when {
                NodeUtil.findPreorder(this, { it.isImportSpecs }, { true }) != null -> {
                    this.getModuleImportSpecsAsList().associate { spec ->
                        val realName = spec.getImportSpecName()
                        val aliases = spec.getImportSpecAliases()
                        aliases to pFile.findEntityInFile(realName)
                    }
                }
                NodeUtil.findPreorder(this, { it.isImportStar }, { true }) != null -> {
                    val aliases = this.getImportSpecAliases()
                    mapOf(aliases to pFile)
                }
                // For example: import foo from "bar"
                else -> {
                    val realName = this.getImportSpecName()
                    mapOf(realName to pFile.findEntityInFile(realName))
                }
            }
        } catch (e: Exception) {
            logger.error { e.toString() }
            return emptyMap()
        }
    }

    private fun makePathFromImport(importText: String): String? {
        val relPath = importText + if (importText.endsWith(".js")) "" else ".js"
        // If import text doesn't contain "/", then it is NodeJS stdlib import.
        if (!relPath.contains("/")) return null
        return Paths.get(File(basePath).parent).resolve(Paths.get(relPath)).pathString
    }

    private fun Node.findEntityInFile(key: String?): Node {
       return key?.let { k ->
            findClass(k, this)
                ?: findFunction(k, this)
                ?: throw ClassNotFoundException("Could not locate entity $k in ${this.sourceFileName}")
        } ?: this
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
