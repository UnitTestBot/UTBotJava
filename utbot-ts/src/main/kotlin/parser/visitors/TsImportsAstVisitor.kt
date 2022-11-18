package parser.visitors

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

    override fun visitImportDeclarationNode(node: ImportDeclarationNode): Boolean {
        val relPath = node.importText + if (node.importText.endsWith(".ts")) "" else ".ts"
        // If import text doesn't contain "/", then it is NodeJS stdlib import.
        if (!relPath.contains("/")) return true
        val finalPath = Paths.get(File(basePath).parent).resolve(Paths.get(relPath))
        val fileText = finalPath.toFile().readText()
        node.nameBindings.forEach {aliases ->
            if (_parsedFilesCache.putIfAbsent(aliases, parser.parse(fileText)) != null)
                throw UnsupportedOperationException("Multiple files for one aliases")
        }
        return true
    }
}