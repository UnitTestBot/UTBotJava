package org.utbot.contest

import org.utbot.framework.codegen.Import
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.plugin.api.UtMethod
import mu.KotlinLogging
import org.utbot.common.FileUtil

private val logger = KotlinLogging.logger {}

class TestClassWriter(
    private val cut: ClassUnderTest,
) {
    private val tab = "    "
    private var tabsAmount = 0

    private val importsBuilder = StringBuilder()

    @Suppress("JoinDeclarationAndAssignment")
    private val codeBuilder: StringBuilder
    private val sbCapacity: Int = 500
    private val existingImports = mutableSetOf<Import>()

    // map from method name to its test methods' code
    private val methodToTests: MutableMap<String, MutableList<String>> = mutableMapOf()
    private val utilMethods: MutableList<String> = mutableListOf()

    private val lineSeparator = System.lineSeparator()
    private val methodsSeparator: String = "$lineSeparator$lineSeparator"

    init {
        codeBuilder = StringBuilder(
            """
            
            public class ${cut.testClassSimpleName} {
                
            }
        """.trimIndent()
        )
    }


    private fun addImports(imports: String) {
        importsBuilder.appendLine(imports)
    }

    private fun addMethod(name: String, method: String) {
        methodToTests[name]?.let {
            it += method
        } ?: run {
            methodToTests[name] = mutableListOf(method)
        }
    }

    fun addImports(imports: List<Import>) {
        imports.filter { it !in existingImports }.takeIf { it.isNotEmpty() }?.let { newImports ->
            addImports(newImports.toText())
            existingImports += newImports
        }
    }

    fun addTest(methodUnderTest: UtMethod<*>, method: String) {
        addMethod(methodUnderTest.callable.name, reformatMethod(method))
    }

    fun addUtilMethod(method: String) {
        utilMethods += method
    }

    private fun List<Import>.toText(): String =
        joinToString(lineSeparator) {
            @Suppress("REDUNDANT_ELSE_IN_WHEN")
            when (it) {
                is StaticImport -> "import static ${it.qualifiedName};"
                else -> "import ${it.qualifiedName};"
            }
        }

    fun writeTestClass() {
        val targetDir = cut.generatedTestFile.parentFile
        targetDir.mkdirs()

        val tests = methodToTests.flatMap { (_, tests) -> tests }.joinToString(methodsSeparator)
        val utils = utilMethods.joinToString(methodsSeparator)
        codeBuilder.run {
            insert(0, importsBuilder)

            if (cut.packageName.isNotEmpty())
                insert(0, "package ${cut.packageName};$lineSeparator$lineSeparator")

            insert(lastIndexOf("}"), tests)
            insert(lastIndexOf("}"), utils)
        }
        logger.info { "File size for ${cut.testClassSimpleName}: ${FileUtil.byteCountToDisplaySize(codeBuilder.length.toLong())}" }
        cut.generatedTestFile.writeText(codeBuilder.toString(), charset)
    }

    private fun reformatMethod(method: String): String = buildString(sbCapacity) {
        tabbed(1) {
            method.lines().map { it.trim() }.apply {
                take(2).forEach {
                    line(it)
                }
                tabbed(1) {
                    subList(2, lastIndex).forEach {
                        line(it)
                    }
                }
                line(last())
            }
            line()
        }
    }

    private fun <R> tabbed(@Suppress("SameParameterValue") tabs: Int, block: () -> R): R {
        val initialTabsAmount = tabsAmount
        try {
            tabsAmount += tabs
            return block()
        } finally {
            tabsAmount = initialTabsAmount
        }
    }

    private fun StringBuilder.line() = appendLine()

    private fun StringBuilder.line(text: String) = lineTabbed(tabsAmount, text)

    private fun StringBuilder.lineTabbed(tabs: Int, text: String) {
        repeat(tabs) { append(tab) }
        append("$text\n")
    }
}
