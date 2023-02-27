package org.utbot.framework.codegen.services.language

import org.utbot.framework.codegen.domain.ProjectType
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
import org.utbot.framework.codegen.tree.CgSpringVariableConstructor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage

abstract class CgLanguageAssistant {

    companion object {
        fun getByCodegenLanguage(language: CodegenLanguage) = when (language) {
            CodegenLanguage.JAVA -> JavaCgLanguageAssistant
            CodegenLanguage.KOTLIN -> KotlinCgLanguageAssistant
            else -> throw UnsupportedOperationException()
        }
    }

    open val outerMostTestClassContent: TestClassContext? = null

    abstract val extension: String

    abstract val languageKeywords: Set<String>

    abstract fun testClassName(
        testClassCustomName: String?,
        testClassPackageName: String,
        classUnderTest: ClassId
    ): Pair<String, String>

    open fun getNameGeneratorBy(context: CgContext): CgNameGenerator = CgNameGeneratorImpl(context)
    open fun getCallableAccessManagerBy(context: CgContext): CgCallableAccessManager =
        CgCallableAccessManagerImpl(context)
    open fun getStatementConstructorBy(context: CgContext): CgStatementConstructor = CgStatementConstructorImpl(context)

    open fun getVariableConstructorBy(context: CgContext): CgVariableConstructor = when (context.projectType) {
            ProjectType.Spring -> CgSpringVariableConstructor(context)
            else -> CgVariableConstructor(context)
        }

    open fun getMethodConstructorBy(context: CgContext): CgMethodConstructor = CgMethodConstructor(context)
    open fun getCgFieldStateManager(context: CgContext): CgFieldStateManager = CgFieldStateManagerImpl(context)

    abstract fun getLanguageTestFrameworkManager(): LanguageTestFrameworkManager
    abstract fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer
}
