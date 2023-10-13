package org.utbot.framework.codegen.services.language

import org.utbot.framework.codegen.renderer.CgPrinter
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.renderer.CgJavaRenderer
import org.utbot.framework.codegen.renderer.CgRendererContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.JVMTestFrameworkManager
import org.utbot.framework.plugin.api.utils.ClassNameUtils.generateTestClassName

object JavaCgLanguageAssistant : AbstractCgLanguageAssistant() {

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
        return generateTestClassName(testClassCustomName, testClassPackageName, classUnderTest)
    }

    override fun getLanguageTestFrameworkManager() = JVMTestFrameworkManager()

    override fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer =
        CgJavaRenderer(context, printer)
}