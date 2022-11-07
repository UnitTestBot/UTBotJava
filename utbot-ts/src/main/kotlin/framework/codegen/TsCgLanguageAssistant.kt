package framework.codegen

import framework.codegen.model.constructor.tree.TsCgCallableAccessManager
import framework.codegen.model.constructor.tree.TsCgMethodConstructor
import framework.codegen.model.constructor.tree.TsCgStatementConstructor
import framework.codegen.model.constructor.tree.TsCgVariableConstructor
import framework.codegen.model.constructor.visitor.CgTsRenderer
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
        "abstract", "arguments", "await", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
        "debugger", "default", "delete", "do", "double", "else", "enum", "eval", "export", "extends", "false", "final",
        "finally", "float", "for", "function", "goto", "if", "implements", "import", "in", "instanceof", "int", "interface",
        "let", "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof", "var", "void",
        "volatile", "while", "with", "yield"
    )

    override fun testClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId
    ): Pair<String, String> {
        return testClassNameGenerator(testClassCustomName, testClassPackageName, classUnderTest)
    }

    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer = CgTsRenderer(context, printer)
    override fun getCallableAccessManagerBy(context: CgContext) = TsCgCallableAccessManager(context)
    override fun getMethodConstructorBy(context: CgContext) = TsCgMethodConstructor(context)
    override fun getStatementConstructorBy(context: CgContext) = TsCgStatementConstructor(context)
    override fun getVariableConstructorBy(context: CgContext) = TsCgVariableConstructor(context)
    override fun getLanguageTestFrameworkManager() = TsTestFrameworkManager()
}