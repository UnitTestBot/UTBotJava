package providers.imports

import service.ContextOwner
import service.ServiceContext
import settings.JsTestGenerationSettings.fileUnderTestAliases

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
        appendLine("const fs = require(\"fs\")\n")
    }

}