package org.utbot.framework.codegen.services.language

import org.utbot.framework.codegen.renderer.CgPrinter
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.renderer.CgKotlinRenderer
import org.utbot.framework.codegen.renderer.CgRendererContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.JVMTestFrameworkManager
import org.utbot.framework.plugin.api.utils.ClassNameUtils.generateTestClassName

object KotlinCgLanguageAssistant : AbstractCgLanguageAssistant() {

    override val extension: String
        get() = ".kt"

    override val languageKeywords: Set<String> = setOf(
        "as", "as?", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "!in", "interface",
        "is", "!is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
        "typeof", "val", "var", "when", "while"
    )

    override fun testClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId
    ): Pair<String, String> {
        return generateTestClassName(testClassCustomName, testClassPackageName, classUnderTest)
    }

    override fun getLanguageTestFrameworkManager() = JVMTestFrameworkManager()

    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer =
        CgKotlinRenderer(context, printer)

    @Suppress("unused")
    private val kotlinSoftKeywords = setOf(
        "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get", "import", "init",
        "param", "property", "receiver", "set", "setparam", "value", "where"
    )

    @Suppress("unused")
    private val kotlinModifierKeywords = setOf(
        "actual", "abstract", "annotation", "companion", "const", "crossinline", "data", "enum", "expect", "external",
        "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator", "out", "override",
        "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg"
    )
}