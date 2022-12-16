package org.utbot.language.ts.framework.codegen

import org.utbot.language.ts.framework.codegen.model.constructor.tree.TsCgCallableAccessManager
import org.utbot.language.ts.framework.codegen.model.constructor.tree.TsCgMethodConstructor
import org.utbot.language.ts.framework.codegen.model.constructor.tree.TsCgStatementConstructor
import org.utbot.language.ts.framework.codegen.model.constructor.tree.TsCgVariableConstructor
import org.utbot.language.ts.framework.codegen.model.constructor.visitor.CgTsRenderer
import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CgLanguageAssistant
import org.utbot.framework.plugin.api.utils.testClassNameGenerator

object TsCgLanguageAssistant : CgLanguageAssistant() {

    override val outerMostTestClassContent: TestClassContext = TestClassContext()

    override val extension: String
        get() = ".ts"

    override val languageKeywords: Set<String> = setOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else",
        "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof",
        "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void",
        "while", "with", "as", "implements", "interface", "let", "package", "private", "protected", "public",
        "static", "yield", "any", "boolean", "constructor", "declare", "get", "module", "require", "number",
        "set", "string", "symbol", "type", "from", "of"
    )

    override fun testClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId
    ): Pair<String, String> {
        return testClassNameGenerator(testClassCustomName, testClassPackageName, classUnderTest)
    }

    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer =
        CgTsRenderer(context, printer)

    override fun getCallableAccessManagerBy(context: CgContext) = TsCgCallableAccessManager(context)
    override fun getMethodConstructorBy(context: CgContext) = TsCgMethodConstructor(context)
    override fun getStatementConstructorBy(context: CgContext) = TsCgStatementConstructor(context)
    override fun getVariableConstructorBy(context: CgContext) = TsCgVariableConstructor(context)
    override fun getLanguageTestFrameworkManager() = TsTestFrameworkManager()
}
