package framework.codegen

import framework.codegen.model.constructor.tree.JsCgCallableAccessManager
import framework.codegen.model.constructor.tree.JsCgMethodConstructor
import framework.codegen.model.constructor.tree.JsCgStatementConstructor
import framework.codegen.model.constructor.tree.JsCgVariableConstructor
import framework.codegen.model.constructor.tree.MochaManager
import framework.codegen.model.constructor.visitor.CgJsRenderer
import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodeGenLanguage
import org.utbot.framework.plugin.api.utils.testClassNameGenerator

object JsCodeLanguage : CodeGenLanguage() {
    override val outerMostTestClassContent: TestClassContext = TestClassContext()
    override val displayName: String = "JavaScript"
    override val id: String = "JavaScript"

    override val extension: String
        get() = ".js"

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

    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer = CgJsRenderer(context, printer)

    override fun getCallableAccessManagerBy(context: CgContext) = JsCgCallableAccessManager(context)

    override fun getMethodConstructorBy(context: CgContext) = JsCgMethodConstructor(context)

    override fun getStatementConstructorBy(context: CgContext) = JsCgStatementConstructor(context)

    override fun getVariableConstructorBy(context: CgContext) = JsCgVariableConstructor(context)

    override val testFrameworks = listOf(Mocha)

    override fun managerByFramework(context: CgContext): TestFrameworkManager = when (context.testFramework) {
        is Mocha -> MochaManager(context)
        else -> throw UnsupportedOperationException("Incorrect TestFramework ${context.testFramework}")
    }

    override val defaultTestFramework = Mocha
}