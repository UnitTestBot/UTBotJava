package org.utbot.language.ts.service

import org.utbot.language.ts.api.TsImport
import org.utbot.language.ts.parser.ast.AstNode
import org.utbot.language.ts.settings.TsDynamicSettings

data class TsServiceContext(
    val utbotDir: String,
    val projectPath: String,
    val filePathToInference: String,
    val parsedFile: AstNode,
    val settings: TsDynamicSettings,
    val imports: List<TsImport>,
    val parsedFiles: Map<String, AstNode>,
)