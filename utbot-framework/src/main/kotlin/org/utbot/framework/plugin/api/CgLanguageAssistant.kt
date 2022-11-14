package org.utbot.framework.plugin.api

import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.name.CgNameGenerator
import org.utbot.framework.codegen.model.constructor.name.CgNameGeneratorImpl
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManager
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManagerImpl
import org.utbot.framework.codegen.model.constructor.tree.CgFieldStateManager
import org.utbot.framework.codegen.model.constructor.tree.CgFieldStateManagerImpl
import org.utbot.framework.codegen.model.constructor.tree.CgMethodConstructor
import org.utbot.framework.codegen.model.constructor.tree.CgVariableConstructor
import org.utbot.framework.codegen.model.constructor.util.CgStatementConstructor
import org.utbot.framework.codegen.model.constructor.util.CgStatementConstructorImpl
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext

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
    open fun getVariableConstructorBy(context: CgContext): CgVariableConstructor = CgVariableConstructor(context)
    open fun getMethodConstructorBy(context: CgContext): CgMethodConstructor = CgMethodConstructor(context)
    open fun getCgFieldStateManager(context: CgContext): CgFieldStateManager = CgFieldStateManagerImpl(context)

    abstract fun getLanguageTestFrameworkManager(): LanguageTestFrameworkManager
    abstract fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer
}
