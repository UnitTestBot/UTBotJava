package org.utbot.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import mu.KotlinLogging
import org.utbot.common.PathUtil.toPath
import org.utbot.common.filterWhen
import org.utbot.engine.Mocker
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.isAbstract
import org.utbot.framework.plugin.api.util.isSynthetic
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.util.isKnownImplicitlyDeclaredMethod
import org.utbot.sarif.SarifReport
import org.utbot.sarif.SourceFindingStrategyDefault
import java.nio.file.Files
import java.nio.file.Paths
import java.time.temporal.ChronoUnit


private val logger = KotlinLogging.logger {}

class GenerateTestsCommand :
    GenerateTestsAbstractCommand(name = "generate", help = "Generates tests for the specified class") {
    private val targetClassFqn by argument(
        help = "Target class fully qualified name"
    )

    private val output by option("-o", "--output", help = "Specifies output file for a generated test")
        .check("Must end with .java or .kt suffix") {
            it.endsWith(".java") or it.endsWith(".kt")
        }

    override val classPath by option(
        "-cp", "--classpath",
        help = "Specifies the classpath for a class under test"
    )
        .required()

    private val sourceCodeFile by option(
        "-s", "--source",
        help = "Specifies source code file for a generated test"
    )
        .check("Must exist and end with .java or .kt suffix") {
            (it.endsWith(".java") || it.endsWith(".kt")) && Files.exists(Paths.get(it))
        }

    private val projectRoot by option(
        "--project-root",
        help = "Specifies the root of the relative paths in the sarif report that are required to show links correctly"
    )

    private val sarifReport by option(
        "--sarif",
        help = "Specifies output file for the static analysis report"
    )
        .check("Must end with *.sarif suffix") {
            it.endsWith(".sarif")
        }

    override val codegenLanguage by option("-l", "--language", help = "Defines the codegen language")
        .choice(
            CodegenLanguage.JAVA.toString() to CodegenLanguage.JAVA,
            CodegenLanguage.KOTLIN.toString() to CodegenLanguage.KOTLIN
        )
        .default(CodegenLanguage.defaultItem)
        .check("Output file extension must match the test class language") { language ->
            output?.let { "." + it.substringAfterLast('.') == language.extension } ?: true
        }

    private val printToStdOut by option(
        "-p",
        "--print-test",
        help = "Specifies whether a test should be printed out to StdOut"
    )
        .flag(default = false)

    override fun run() {
        val started = now()
        val workingDirectory = getWorkingDirectory(targetClassFqn)
            ?: throw Exception("Cannot find the target class in the classpath")

        try {
            logger.debug { "Generating test for [$targetClassFqn] - started" }
            logger.debug { "Classpath to be used: ${newline()} $classPath ${newline()}" }

            // utContext is used in `targetMethods`, `generate`, `generateTest`, `generateReport`
            withUtContext(UtContext(classLoader)) {
                val classIdUnderTest = ClassId(targetClassFqn)
                val targetMethods = classIdUnderTest.targetMethods()
                    .filterWhen(UtSettings.skipTestGenerationForSyntheticAndImplicitlyDeclaredMethods) {
                        !it.isSynthetic && !it.isKnownImplicitlyDeclaredMethod
                    }
                    .filterNot { it.isAbstract }
                val testCaseGenerator = initializeGenerator(workingDirectory)

                if (targetMethods.isEmpty()) {
                    throw Exception("Nothing to process. No methods were provided")
                }

                val testClassName = output?.toPath()?.toFile()?.nameWithoutExtension
                    ?: "${classIdUnderTest.simpleName}Test"
                val testSets = generateTestSets(
                    testCaseGenerator,
                    targetMethods,
                    sourceCodeFile?.let(Paths::get),
                    searchDirectory = workingDirectory,
                    chosenClassesToMockAlways = (Mocker.defaultSuperClassesToMockAlwaysNames + classesToMockAlways)
                        .mapTo(mutableSetOf()) { ClassId(it) }
                )
                val testClassBody = generateTest(classIdUnderTest, testClassName, testSets)

                if (printToStdOut) {
                    logger.info { testClassBody }
                }
                if (sarifReport != null) {
                    generateReport(targetClassFqn, testSets, testClassBody)
                }
                saveToFile(testClassBody, output)
            }
        } catch (t: Throwable) {
            logger.error { "An error has occurred while generating test for snippet $targetClassFqn : $t" }
            throw t
        } finally {
            val duration = ChronoUnit.MILLIS.between(started, now())
            logger.debug { "Generating test for [$targetClassFqn] - completed in [$duration] (ms)" }
        }
    }

    private fun generateReport(classFqn: String, testSets: List<UtMethodTestSet>, testClassBody: String) = try {
        // reassignments for smart casts
        val testsFilePath = output
        val projectRootPath = projectRoot

        when {
            testsFilePath == null -> {
                println("The output file is required to generate a report. Please, specify \"--output\" option.")
            }
            projectRootPath == null -> {
                println("The path to the project root is required to generate a report. Please, specify \"--project-root\" option.")
            }
            sourceCodeFile == null -> {
                println("The source file is not found. Please, specify \"--source\" option.")
            }
            else -> {
                val sourceFinding =
                    SourceFindingStrategyDefault(classFqn, sourceCodeFile!!, testsFilePath, projectRootPath)
                val report = SarifReport(testSets, testClassBody, sourceFinding).createReport().toJson()
                saveToFile(report, sarifReport)
                println("The report was saved to \"$sarifReport\".")
            }
        }
    } catch (t: Throwable) {
        logger.error { "An error has occurred while generating sarif report for snippet $targetClassFqn : $t" }
        throw t
    }
}