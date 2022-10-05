package org.utbot.framework.plugin.api

import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.Junit4Manager
import org.utbot.framework.codegen.model.constructor.tree.Junit5Manager
import org.utbot.framework.codegen.model.constructor.tree.TestFrameworkManager
import org.utbot.framework.codegen.model.constructor.tree.TestNgManager
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgKotlinRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext
import org.utbot.framework.plugin.api.utils.testClassNameGenerator

object KotlinCodeLanguage : CodeGenLanguage() {
    override val displayName: String = "Kotlin (experimental)"
    override val id: String = "Kotlin"

    override val extension: String
        get() = ".kt"

    override val languageKeywords: Set<String> = setOf(
        "as", "as?", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "!in", "interface",
        "is", "!is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
        "val", "var", "when", "while"
    )

    override fun testClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId
    ): Pair<String, String> {
        return testClassNameGenerator(testClassCustomName, testClassPackageName, classUnderTest)
    }

    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer = CgKotlinRenderer(context, printer)

    override val testFrameworks = listOf(Junit4, Junit5, TestNg)

    override fun managerByFramework(context: CgContext): TestFrameworkManager = when (context.testFramework) {
        is Junit4 -> Junit4Manager(context)
        is Junit5 -> Junit5Manager(context)
        is TestNg -> TestNgManager(context)
        else -> throw UnsupportedOperationException("Incorrect TestFramework ${context.testFramework}")
    }

    override val defaultTestFramework = Junit5
}