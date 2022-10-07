package org.utbot.framework.plugin.api

import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgKotlinRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext
import org.utbot.framework.plugin.api.utils.testClassNameGenerator

object KotlinCgLanguageAssistant : CgLanguageAssistant() {

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

    override fun getLanguageTestFrameworkManager() = JVMTestFrameworkManager()

    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer = CgKotlinRenderer(context, printer)
}