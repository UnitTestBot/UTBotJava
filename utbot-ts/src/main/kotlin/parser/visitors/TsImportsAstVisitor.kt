package parser.visitors

import api.TsImport
import java.io.File
import java.nio.file.Paths
import parser.TsParser
import parser.ast.AstNode
import parser.ast.ImportDeclarationNode

class TsImportsAstVisitor(
    private val basePath: String,
    private val parser: TsParser,
): AbstractAstVisitor() {

    // Used not to parse the same file multiple times.
    private val _parsedFilesCache = mutableMapOf<String, AstNode>()

    val parsedFiles: Map<String, AstNode>
        get() = _parsedFilesCache.toMap()

    // Used for generating temp files.
    private val _importObjects = mutableListOf<TsImport>()

    val importObjects: List<TsImport>
        get() = _importObjects.toList()

    override fun visitImportDeclarationNode(node: ImportDeclarationNode): Boolean {
        val relPath = node.importText + if (node.importText.endsWith(".ts")) "" else ".ts"
        // If import text doesn't contain "/", then it is NodeJS stdlib import.
        if (!relPath.contains("/")) return true
        val finalPath = Paths.get(File(basePath).parent).resolve(Paths.get(relPath))
        val fileText = finalPath.toFile().readText()
        val parsedFile = parser.parse(fileText)
        node.nameBindings.forEach { alias ->
            node.importedNodes[alias] = parsedFile.extractDecl(alias)
            if (_parsedFilesCache.putIfAbsent(alias, parser.parse(fileText)) != null)
                throw UnsupportedOperationException("Multiple files for one aliases")
            _importObjects.add(TsImport(alias, finalPath))

        }
        return true
    }

    private fun AstNode.extractDecl(key: String): AstNode =
        try {
            val classAstVisitor = TsClassAstVisitor(key)
            classAstVisitor.accept(this)
            classAstVisitor.targetClassNode
        } catch (e: Exception) {
            val functionAstVisitor = TsFunctionAstVisitor(key, null)
            functionAstVisitor.accept(this)
            functionAstVisitor.targetFunctionNode
        }
}