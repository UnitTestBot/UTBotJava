package org.utbot.framework.codegen.services.language

import org.utbot.framework.codegen.domain.context.TestClassContext
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.renderer.CgPrinter
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.renderer.CgRendererContext
import org.utbot.framework.codegen.services.CgNameGenerator
import org.utbot.framework.codegen.services.CgNameGeneratorImpl
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.access.CgCallableAccessManagerImpl
import org.utbot.framework.codegen.services.access.CgFieldStateManager
import org.utbot.framework.codegen.services.access.CgFieldStateManagerImpl
import org.utbot.framework.codegen.tree.CgMethodConstructor
import org.utbot.framework.codegen.tree.CgStatementConstructor
import org.utbot.framework.codegen.tree.CgStatementConstructorImpl
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage

interface CgLanguageAssistant {

    companion object {
        fun getByCodegenLanguage(language: CodegenLanguage) = when (language) {
            CodegenLanguage.JAVA -> JavaCgLanguageAssistant
            CodegenLanguage.KOTLIN -> KotlinCgLanguageAssistant
            else -> throw UnsupportedOperationException()
        }
    }

    val outerMostTestClassContent: TestClassContext?

    val extension: String

    val languageKeywords: Set<String>

    fun testClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId
    ): Pair<String, String>

    fun getNameGeneratorBy(context: CgContext): CgNameGenerator
    fun getCallableAccessManagerBy(context: CgContext): CgCallableAccessManager
    fun getStatementConstructorBy(context: CgContext): CgStatementConstructor

    fun getVariableConstructorBy(context: CgContext): CgVariableConstructor

    fun getMethodConstructorBy(context: CgContext): CgMethodConstructor
    fun getCgFieldStateManager(context: CgContext): CgFieldStateManager

    fun getLanguageTestFrameworkManager(): LanguageTestFrameworkManager
    fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer
}

abstract class AbstractCgLanguageAssistant : CgLanguageAssistant {
    override val outerMostTestClassContent: TestClassContext? get() = null

    override fun getNameGeneratorBy(context: CgContext): CgNameGenerator = CgNameGeneratorImpl(context)
    override fun getCallableAccessManagerBy(context: CgContext): CgCallableAccessManager =
        CgCallableAccessManagerImpl(context)
    override fun getStatementConstructorBy(context: CgContext): CgStatementConstructor = CgStatementConstructorImpl(context)

    override fun getVariableConstructorBy(context: CgContext): CgVariableConstructor = CgVariableConstructor(context)

    override fun getMethodConstructorBy(context: CgContext): CgMethodConstructor = CgMethodConstructor(context)
    override fun getCgFieldStateManager(context: CgContext): CgFieldStateManager = CgFieldStateManagerImpl(context)
}
