package providers.imports

import service.ContextOwner
import service.ServiceContext
import settings.JsTestGenerationSettings.fileUnderTestAliases

class ModuleImportsProvider(context: ServiceContext) : IImportsProvider, ContextOwner by context {

    override val ternScriptImports: String = buildString {
        appendLine("import * as tern from \"tern/lib/tern.js\"")
        appendLine("import * as condense from \"tern/lib/condense.js\"")
        appendLine("import * as util from \"tern/test/util.js\"")
        appendLine("import * as fs from \"fs\"")
        appendLine("import * as path from \"path\"")
    }

    override val tempFileImports: String = buildString {
        val importFileUnderTest = "./instr/${filePathToInference.first().substringAfterLast("/")}"
        appendLine("import * as $fileUnderTestAliases from \"$importFileUnderTest\"")
        appendLine("import * as fs from \"fs\"")
    }
}