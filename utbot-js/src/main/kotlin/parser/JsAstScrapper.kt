package parser

import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.jscomp.SourceFile
import com.google.javascript.rhino.Node
import framework.codegen.ModuleType
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import mu.KotlinLogging
import org.json.JSONObject
import parser.JsParserUtils.getAbstractFunctionName
import parser.JsParserUtils.getClassMethods
import parser.JsParserUtils.getImportSpecAliases
import parser.JsParserUtils.getImportSpecName
import parser.JsParserUtils.getModuleImportSpecsAsList
import parser.JsParserUtils.getModuleImportText
import parser.JsParserUtils.getRequireImportText
import parser.JsParserUtils.isAnyVariableDecl
import parser.JsParserUtils.isRequireImport
import parser.visitors.IAstVisitor
import parser.visitors.JsClassAstVisitor
import parser.visitors.JsFunctionAstVisitor
import parser.visitors.JsVariableAstVisitor
import settings.JsDynamicSettings
import utils.JsCmdExec
import utils.PathResolver
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

class JsAstScrapper(
    private val parsedFile: Node,
    private val basePath: String,
    private val tempUtbotPath: Path,
    private val moduleType: ModuleType,
    private val settings: JsDynamicSettings
) {

    // Used not to parse the same file multiple times.
    private val _parsedFilesCache = mutableMapOf<Path, Node>(Paths.get(basePath) to parsedFile)
    private val _filesToInfer: MutableList<String> = mutableListOf(basePath)
    val filesToInfer: List<String>
        get() = _filesToInfer.toList()
    private val _importsMap = mutableMapOf<String, Node>()
    val importsMap: Map<String, Node>
        get() = _importsMap.toMap()

    init {
        _importsMap.apply {
            val processedFiles = mutableSetOf<Path>()
            fun Node.collectImportsRec(): List<Pair<String, Node>> {
                processedFiles.add(Paths.get(this.sourceFileName!!))
                val vis = Visitor()
                vis.accept(this)
                return vis.importNodes.flatMap { node ->
                    val temp = node.importedNodes()
                    temp.toList() + temp.flatMap { entry ->
                        val path = Paths.get(entry.value.sourceFileName!!)
                        if (!processedFiles.contains(path)) {
                            path.toFile().parseIfNecessary().collectImportsRec()
                        } else emptyList()
                    }
                }
            }
            this.putAll(parsedFile.collectImportsRec().toMap())
            this.toMap()
        }
    }

    fun findFunction(key: String, file: Node): Node? {
        if (_importsMap[key]?.isFunction == true) return _importsMap[key]
        val functionVisitor = JsFunctionAstVisitor(key, null)
        functionVisitor.accept(file)
        return try {
            functionVisitor.targetFunctionNode
        } catch (e: Exception) {
            null
        }
    }

    fun findClass(key: String, file: Node): Node? {
        if (_importsMap[key]?.isClass == true) return _importsMap[key]
        val classVisitor = JsClassAstVisitor(key)
        classVisitor.accept(file)
        return try {
            classVisitor.targetClassNode
        } catch (e: Exception) {
            null
        }
    }

    fun findMethod(classKey: String, methodKey: String, file: Node): Node? {
        val classNode = findClass(classKey, file)
        return classNode?.getClassMethods()?.find { it.getAbstractFunctionName() == methodKey }
    }

    fun findVariable(key: String, file: Node): Node? {
        if (_importsMap[key]?.isAnyVariableDecl() == true) return _importsMap[key]
        val variableVisitor = JsVariableAstVisitor(key)
        variableVisitor.accept(file)
        return try {
            variableVisitor.targetVariableNode
        } catch (e: Exception) {
            null
        }
    }

    private fun File.parseIfNecessary(): Node =
        _parsedFilesCache.getOrPut(this.toPath()) {
            _filesToInfer += this.path.replace("\\", "/")
            Compiler().parse(SourceFile.fromCode(this.path, readText()))
        }

    private fun Node.importedNodes(): Map<String, Node> {
        return when {
            this.isRequireImport() -> this.processRequireImport()
            this.isImport -> this.processModuleImport()
            else -> emptyMap()
        }
    }

    @Suppress("unchecked_cast")
    private fun getExportedKeys(importPath: String): List<String> {
//        val importPath = PathResolver.getRelativePath(pathToTempFile.pathString, basePath)
        val pathToTempFile = Paths.get(tempUtbotPath.pathString + "/exp_temp.js")
        val text = buildString {
            when (moduleType) {
                ModuleType.COMMONJS -> {
                    appendLine("const temp = require(\"$importPath\")")
                    appendLine("const fs = require(\"fs\")")
                }

                else -> {
                    appendLine("import * as temp from \"$importPath\"")
                    appendLine("import * as fs from \"fs\"")
                }
            }
            appendLine("fs.writeFileSync(\"exp_temp.json\", JSON.stringify({files: Object.keys(temp)}))")
        }
        pathToTempFile.toFile().writeText(text)
        JsCmdExec.runCommand(
            cmd = arrayOf("\"${settings.pathToNode}\"", pathToTempFile.pathString),
            dir = pathToTempFile.parent.pathString,
            shouldWait = true,
            timeout = settings.timeout,
        )
        val pathToJson = pathToTempFile.pathString.replace(".js", ".json")
        return JSONObject(File(pathToJson).readText()).getJSONArray("files").toList() as List<String>
    }

    private fun Node.processRequireImport(): Map<String, Node> {
        try {
            val pathToFile = makePathFromImport(this.getRequireImportText()) ?: return emptyMap()
            val pFile = File(pathToFile).parseIfNecessary()
            val objPattern = NodeUtil.findPreorder(this.parent, { it.isObjectPattern }, { true })
            return when {
                objPattern != null -> {
                    buildList {
                        var currNode: Node? = objPattern.firstChild!!
                        while (currNode != null) {
                            add(currNode.string)
                            currNode = currNode.next
                        }
                    }.associateWith { key -> pFile.findEntityInFile(key) }
                }
                else -> {
                    val importPath = PathResolver.getRelativePath(tempUtbotPath.pathString, pathToFile)
                    getExportedKeys(importPath).associateWith { key ->
                        pFile.findEntityInFile(key)
                    }
                }
            }
        } catch (e: ClassNotFoundException) {
            logger.error { e.toString() }
            return emptyMap()
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
                    // Do we need to know alias for "*" import?
                    val aliases = this.getImportSpecAliases()
                    val importPath = PathResolver.getRelativePath(tempUtbotPath.pathString, pathToFile)
                    getExportedKeys(importPath).associateWith { key ->
                        pFile.findEntityInFile(key)
                    }
                }
                // For example: import foo from "bar"
                else -> {
                    val realName = this.getImportSpecName()
                    mapOf(realName to pFile.findEntityInFile(realName))
                }
            }
        } catch (e: ClassNotFoundException) {
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
                ?: findVariable(k, this)
                ?: throw ClassNotFoundException("Could not locate entity $k in ${this.sourceFileName}")
        } ?: this
    }

    private class Visitor : IAstVisitor {

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
