package providers.imports

import service.ContextOwner
import service.ServiceContext
import settings.JsTestGenerationSettings.fileUnderTestAliases
import utils.PathResolver

class RequireImportsProvider(context: ServiceContext) : IImportsProvider, ContextOwner by context {

    override val ternScriptImports: String = buildString {
        appendLine("const tern = require(\"tern/lib/tern\")")
        appendLine("const condense = require(\"tern/lib/condense.js\")")
        appendLine("const util = require(\"tern/test/util.js\")")
        appendLine("const fs = require(\"fs\")")
        appendLine("const path = require(\"path\")")
    }

    override val tempFileImports: String = buildString {
        val importFileUnderTest = "instr/${filePathToInference.first().substringAfterLast("/")}"
        appendLine("const $fileUnderTestAliases = require(\"./$importFileUnderTest\")")
        val currDir = "${projectPath}/${utbotDir}"
        for ((key, value) in necessaryImports) {
            val importPath = PathResolver.getRelativePath(currDir, value.sourceFileName!!)
            appendLine("const {$key} = require(\"$importPath\")")
        }
        appendLine("const fs = require(\"fs\")\n")
    }

}
