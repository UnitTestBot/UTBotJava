package org.utbot.cli

import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.util.GeneratedSarif
import org.utbot.framework.util.Snippet
import org.utbot.framework.util.compileClassFile
import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class GenerateTestsCommandTest {
    private val className = "MathUtils"

    private val javaSnippet = """
        public class $className {
            public static int max(int a, int b) {
                return a > b ? a : b;
            }
        }
    """.trimIndent()

    private val javaSnippetError = """
        public class $className {
            public static int multiply() {
                // ArithmeticException
                return Math.multiplyExact(100000, 100000);
            }
        }""".trimIndent()

    private val snippets =
        listOf(Snippet(CodegenLanguage.JAVA, javaSnippet), Snippet(CodegenLanguage.KOTLIN, javaSnippet))

    @Test
    fun positiveCase_jUnit4() {
        snippets.forEach { snippet ->
            performChecks(
                testFramework = "junit4",
                staticMocksOption = "mock-statics",
                snippet,
                { test -> test.hasStaticImport("org.junit.Assert.assertEquals") },
                { test -> test.doesntHaveStaticImport("org.junit.jupiter.api.Assertions.assertEquals") },
                { test -> test.doesntHaveStaticImport("org.testng.Assert.assertEquals") },
                { test -> test.hasImport("org.junit.Test") },
                { test -> test.doesntHaveImport("org.junit.jupiter.api.Test") },
                { test -> test.hasLine("@Test") },
                { test -> test.doesntHaveImport("org.testng.annotations.Test") },
            )
        }
    }

    @Test
    fun positiveCase_jUnit5() {
        snippets.forEach { snippet ->
            performChecks(
                testFramework = "junit5",
                staticMocksOption = "mock-statics",
                snippet,
                { test -> test.doesntHaveStaticImport("org.junit.Assert.assertEquals") },
                { test -> test.hasStaticImport("org.junit.jupiter.api.Assertions.assertEquals") },
                { test -> test.doesntHaveStaticImport("org.testng.Assert.assertEquals") },
                { test -> test.doesntHaveImport("org.junit.Test") },
                { test -> test.hasImport("org.junit.jupiter.api.Test") },
                { test -> test.doesntHaveImport("org.testng.annotations.Test") },
                { test -> test.hasLine("@Test") },
            )
        }
    }

    @Test
    fun positiveCase_testng() {
        snippets.forEach { snippet ->
            performChecks(
                testFramework = "testng",
                staticMocksOption = "mock-statics",
                snippet,
                { test -> test.doesntHaveStaticImport("org.junit.Assert.assertEquals") },
                { test -> test.doesntHaveStaticImport("org.junit.jupiter.api.Assertions.assertEquals") },
                { test -> test.hasStaticImport("org.testng.Assert.assertEquals") },
                { test -> test.doesntHaveImport("org.junit.Test") },
                { test -> test.doesntHaveImport("org.junit.jupiter.api.Test") },
                { test -> test.hasImport("org.testng.annotations.Test") },
                { test -> test.hasLine("@Test") },
            )
        }
    }

    @Test
    fun positiveCase_sarif() {
        snippets.forEach { snippet ->
            performSarifChecks(
                testFramework = "junit4",
                staticMocksOption = "mock-statics",
                snippet,
                { report -> report.hasSchema() },
                { report -> report.hasVersion() },
                { report -> report.hasRules() },
                { report -> report.hasResults() },
            )
        }
    }

    @Test
    fun negativeCase_sarif() {
        performSarifChecks(
            testFramework = "junit4",
            staticMocksOption = "mock-statics",
            Snippet(CodegenLanguage.JAVA, javaSnippetError),
            { report -> report.hasSchema() },
            { report -> report.hasVersion() },
            { report -> report.hasRules() },
            { report -> report.hasResults() },
            { report -> report.hasCodeFlows() },
            { report -> report.codeFlowsIsNotEmpty() }, // stackTrace is not empty
        )
    }

    private fun performChecks(
        testFramework: String,
        staticMocksOption: String,
        snippet: Snippet,
        vararg matchers: (Snippet) -> Boolean
    ) {
        val result = snippet.copy(text = runTestGeneration(testFramework, staticMocksOption, snippet))
        assertFalse(result.text.isEmpty(), "A Non-empty test case should be generated")
        matchers.forEachIndexed { index, matcher ->
            if (!matcher(result)) fail { "Matcher $index failed.\r\nCode:\r\n$snippet\r\n\r\nTest:$result" }
        }
    }

    private fun performSarifChecks(
        testFramework: String,
        staticMocksOption: String,
        snippet: Snippet,
        vararg matchers: (GeneratedSarif) -> Boolean
    ) {
        val result = GeneratedSarif(runSarifGeneration(testFramework, staticMocksOption, snippet))
        assertFalse(result.text.isEmpty(), "A Non-empty sarif report should be generated")
        matchers.forEachIndexed { index, matcher ->
            if (!matcher(result)) fail { "Matcher $index failed.\r\nCode:\r\n$snippet\r\n\r\nSarif:$result" }
        }
    }

    private fun getCommands(testFramework: String, staticMocksOption: String, snippet: Snippet, withSarif: Boolean): List<String> {
        val classUnderTest = compileClassFile(className, snippet).toString()
        val classPath = classUnderTest.substringBeforeLast(File.separator)
        val outputFile = classUnderTest.replace(".class", "Test${snippet.codegenLanguage.extension}")
        val sourceCodeFile = classUnderTest.replace(".class", CodegenLanguage.JAVA.extension)
        val sarifFile = "$classPath/utbot.sarif"

        val cmd = mutableListOf(
            className,
            "-cp", classPath,
            "-s", sourceCodeFile,
            "-o", outputFile,
            "--test-framework", testFramework,
            "-l", snippet.codegenLanguage.toString(),
            "--mock-statics", staticMocksOption
        )
        if (withSarif)
            cmd.addAll(
                listOf(
                    "--project-root", classPath,
                    "--sarif", sarifFile
                )
            )
        return cmd
    }

    private fun runTestGeneration(testFramework: String, staticMocksOption: String, snippet: Snippet): String {
        val command = GenerateTestsCommand()
        val argumentList = getCommands(testFramework, staticMocksOption, snippet, withSarif = false)

        val outputFile = argumentList.getArgumentAfter(option = "-o")

        command.main(argumentList)

        return readOutput(outputFile)
    }

    private fun runSarifGeneration(testFramework: String, staticMocksOption: String, snippet: Snippet): String {
        val command = GenerateTestsCommand()
        val argumentList = getCommands(testFramework, staticMocksOption, snippet, withSarif = true)

        val sarifFile = argumentList.getArgumentAfter(option = "--sarif")

        command.main(argumentList)

        return readOutput(sarifFile)
    }

    private fun readOutput(path: String) = File(path).readText()

    private fun List<String>.getArgumentAfter(option: String): String =
        this[this.indexOf(option) + 1]
}