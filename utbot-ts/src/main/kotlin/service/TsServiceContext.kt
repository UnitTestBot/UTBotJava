package service

import parser.ast.AstNode
import settings.TsDynamicSettings

data class TsServiceContext(
    val utbotDir: String,
    val projectPath: String,
    val filePathToInference: String,
    val parsedFile: AstNode,
    val settings: TsDynamicSettings,
)