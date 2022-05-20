package org.utbot.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import org.utbot.cli.util.junit4RunTests
import org.utbot.cli.util.junit5RunTests
import org.utbot.cli.util.testngRunTests
import org.utbot.cli.writers.ConsoleWriter
import org.utbot.cli.writers.FileWriter
import org.utbot.cli.writers.IWriter
import org.utbot.common.PathUtil
import org.utbot.common.PathUtil.replaceSeparator
import org.utbot.common.PathUtil.toPath
import org.utbot.common.PathUtil.toURL
import org.utbot.common.toPath
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

class RunTestsCommand : CliktCommand(name = "run", help = "Runs tests for the specified class") {
    private val classWithTestsName by argument(
        help = "Specifies a class with tests"
    )

    private val classPath by option(
        "-cp", "--classpath",
        help = "Specifies the classpath for a class with tests"
    )
        .required()

    private val testFramework by option("--test-framework", help = "Test framework to be used")
        .choice("junit4", "junit5", "testng")
        .default("junit4")

    private val output by option(
        "-o", "--output",
        help = "Specifies output file with specified extension for tests run information"
    )

    private val classLoader: URLClassLoader by lazy {
        val urls = classPath
            .split(File.pathSeparator)
            .map { uri ->
                uri.toPath().toURL()
            }
            .toTypedArray()
        URLClassLoader(urls)
    }

    override fun run() {
        val started = now()
        getWorkingDirectory(classWithTestsName)
            ?: throw Exception("Cannot find the target class in the classpath")

        if (output != null) {
            val output = File(output)
            output.createNewFile()
            if (!output.exists()) {
                throw Exception("Output file $output does not exist")
            }
        }

        val writer: IWriter = output?.let { FileWriter(it) } ?: ConsoleWriter()

        try {

            logger.debug { "Running test for [$classWithTestsName] - started" }
            val tests: Class<*> = loadClassBySpecifiedFqn(classWithTestsName)

            runTests(tests, writer)

        } catch (t: Throwable) {
            logger.error { "An error has occurred while running test for $classWithTestsName : $t" }
            throw t
        } finally {
            val duration = ChronoUnit.MILLIS.between(started, now())
            logger.debug { "Running test for [${classWithTestsName}] - completed in [$duration] (ms)" }
            if (writer is FileWriter) {
                writer.close()
            }
        }
    }

    private fun runTests(classWithTests: Class<*>, writer: IWriter) {
        when (testFramework) {
            "junit4" -> {
                junit4RunTests(classWithTests, writer)
            }
            "junit5" -> {
                junit5RunTests(classWithTests, writer)
            }
            else -> {
                testngRunTests(classWithTests, writer)
            }
        }
    }

    private fun getWorkingDirectory(classFqn: String): Path? {
        val classRelativePath = PathUtil.classFqnToPath(classFqn) + ".class"
        val classAbsoluteURL = classLoader.getResource(classRelativePath) ?: return null
        val classAbsolutePath = replaceSeparator(classAbsoluteURL.toPath().toString())
            .removeSuffix(classRelativePath)
        return Paths.get(classAbsolutePath)
    }

    private fun loadClassBySpecifiedFqn(classFqn: String): Class<*> =
        classLoader.loadClass(classFqn)

    private fun now() = LocalDateTime.now()
}
