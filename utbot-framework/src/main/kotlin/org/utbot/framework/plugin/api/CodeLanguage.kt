package org.utbot.framework.plugin.api

import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.constructor.TestClassContext
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.name.CgNameGenerator
import org.utbot.framework.codegen.model.constructor.name.CgNameGeneratorImpl
import org.utbot.framework.codegen.model.constructor.tree.*
import org.utbot.framework.codegen.model.constructor.util.CgStatementConstructor
import org.utbot.framework.codegen.model.constructor.util.CgStatementConstructorImpl
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.codegen.model.visitor.CgRendererContext


object CodegenLanguageProvider {
    val allItems: MutableList<CodeGenLanguage> = emptyList<CodeGenLanguage>().toMutableList()
}

abstract class CodeGenLanguage : CodeGenerationSettingItem {
    open val outerMostTestClassContent: TestClassContext? = null

    override val description: String
        get() = "Generate unit tests in $displayName"

    abstract val extension: String

    abstract val languageKeywords: Set<String>

    override fun toString(): String = displayName

    abstract fun testClassName(testClassCustomName: String?, testClassPackageName: String, classUnderTest: ClassId): Pair<String, String>

    enum class OperatingSystem {
        WINDOWS,
        UNIX;

        companion object {
            fun fromSystemProperties(): OperatingSystem {
                val osName = System.getProperty("os.name")
                return when {
                    osName.startsWith("Windows") -> WINDOWS
                    else -> UNIX
                }
            }
        }
    }

    companion object : CodeGenerationSettingBox {
        override val defaultItem: CodeGenLanguage get() = allItems.first()
        override val allItems: List<CodeGenLanguage> = CodegenLanguageProvider.allItems.toList()
    }

    open fun getNameGeneratorBy(context: CgContext): CgNameGenerator = CgNameGeneratorImpl(context)
    open fun getCallableAccessManagerBy(context: CgContext): CgCallableAccessManager = CgCallableAccessManagerImpl(context)
    open fun getStatementConstructorBy(context: CgContext): CgStatementConstructor = CgStatementConstructorImpl(context)
    open fun getVariableConstructorBy(context: CgContext): CgVariableConstructor = CgVariableConstructor(context)
    open fun getMethodConstructorBy(context: CgContext): CgMethodConstructor = CgMethodConstructor(context)
    abstract fun cgRenderer(context: CgRendererContext, printer: CgPrinter): CgAbstractRenderer

    open val testFrameworks: List<TestFramework> = emptyList()
    abstract fun managerByFramework(context: CgContext): TestFrameworkManager
    abstract val defaultTestFramework: TestFramework
    open var memoryObjects: MutableMap<Long, CgVariable> = emptyMap<Long, CgVariable>().toMutableMap()
}
