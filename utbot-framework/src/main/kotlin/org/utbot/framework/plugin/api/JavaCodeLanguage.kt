package org.utbot.framework.plugin.api

import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.Junit4Manager
import org.utbot.framework.codegen.model.constructor.tree.Junit5Manager
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager
import org.utbot.framework.codegen.model.constructor.tree.TestNgManager
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgJavaRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext
import org.utbot.framework.plugin.api.utils.testClassNameGenerator

object JavaCodeLanguage : CodeGenLanguage() {
    override val displayName: String = "Java"

    override val extension: String
        get() = ".java"

    override val languageKeywords: Set<String> = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto",
        "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
        "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "null", "false", "true"
    )

    override fun testClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId
    ): Pair<String, String> {
        return testClassNameGenerator(testClassCustomName, testClassPackageName, classUnderTest)
    }

    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer = CgJavaRenderer(context, printer)

    override val testFrameworks = listOf(Junit4, Junit5, TestNg)
    override fun managerByFramework(context: CgContext): TestFrameworkManager = when (context.testFramework) {
        is Junit4 -> Junit4Manager(context)
        is Junit5 -> Junit5Manager(context)
        is TestNg -> TestNgManager(context)
        else -> throw UnsupportedOperationException("Incorrect TestFramework ${context.testFramework}")
    }

    override val defaultTestFramework: TestFramework = Junit5
}