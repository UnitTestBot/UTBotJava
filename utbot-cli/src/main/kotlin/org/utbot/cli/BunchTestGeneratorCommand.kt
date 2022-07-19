package org.utbot.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import org.utbot.cli.util.createClassLoader
import org.utbot.engine.Mocker
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

class BunchTestGeneratorCommand : GenerateTestsAbstractCommand(
    name = "bunchGenerate",
    help = "Generates tests for class files in the specified directory"
) {
    private val classRootDirectory by argument(
        help = "Directory with classes"
    )

    override val classPath by option(
        "-cp", "--classpath",
        help = "Specifies the classpath for a class under test"
    )

    private val output by option(
        "-o",
        "--output",
        help = "Specifies output directory. It will be populated with package-directory structure."
    )

    private val packageFilter by option(
        "-pf", "--filter",
        help = "Specifies a string to generate tests only for classes that contains the substring in the fully qualified name"
    )

    private val classPathFilePath by option(
        "--classpathFile",
        help = "Specifies the classpath as a list of strings in a file"
    )

    override val codegenLanguage by option("-l", "--language", help = "Defines the codegen language")
        .choice(
            CodegenLanguage.JAVA.toString() to CodegenLanguage.JAVA,
            CodegenLanguage.KOTLIN.toString() to CodegenLanguage.KOTLIN
        )
        .default(CodegenLanguage.defaultItem)

    override val classLoader: URLClassLoader by lazy {
        assert(classPath != null || classPathFilePath != null) { "Classpath or classpath file have to be specified." }
        if (classPathFilePath != null) {
            createClassLoader(classPath, classPathFilePath)
        } else {
            createClassLoader(classPath)
        }
    }

    override fun run() {
        val classesFromPath = loadClassesFromPath(classLoader, classRootDirectory)
        classesFromPath.filterNot { it.java.isInterface }.filter { clazz ->
            clazz.qualifiedName != null
        }.
        filter { clazz ->
            packageFilter?.run {
                clazz.qualifiedName?.contains(this) ?: false
            } ?: true
        }.forEach {
            if (it.qualifiedName != null) {
                generateTestsForClass(it.qualifiedName!!)
            } else {
                logger.info("No qualified name for $it")
            }
        }
    }

    private fun generateTestsForClass(targetClassFqn: String) {
        val started = now()
        val workingDirectory = getWorkingDirectory(targetClassFqn)
            ?: throw IllegalStateException("Error: Cannot find the target class in the classpath")

        try {
            logger.debug { "Generating test for [$targetClassFqn] - started" }
            logger.debug { "Classpath to be used: ${newline()} $classPath ${newline()}" }

            val classUnderTest: KClass<*> = loadClassBySpecifiedFqn(targetClassFqn)
            val targetMethods = classUnderTest.targetMethods()
            if (targetMethods.isEmpty()) return

            val testCaseGenerator = initializeGenerator(workingDirectory)

            // utContext is used in `generate`, `generateTest`, `generateReport`
            withUtContext(UtContext(classLoader)) {

                val testClassName = "${classUnderTest.simpleName}Test"

                val testSets = generateTestSets(
                    testCaseGenerator,
                    targetMethods,
                    searchDirectory = workingDirectory,
                    chosenClassesToMockAlways = (Mocker.defaultSuperClassesToMockAlwaysNames + classesToMockAlways)
                        .mapTo(mutableSetOf()) { ClassId(it) }
                )

                val testClassBody = generateTest(classUnderTest, testClassName, testSets)

                val outputArgAsFile = File(output ?: "")
                if (!outputArgAsFile.exists()) {
                    outputArgAsFile.mkdirs()
                }

                val outputDir = "$outputArgAsFile${File.separator}"

                val packageNameAsList = classUnderTest.jvmName.split('.').dropLast(1)
                val path = Paths.get("${outputDir}${packageNameAsList.joinToString(separator = File.separator)}")
                path.toFile().mkdirs()

                saveToFile(testClassBody, "$path${File.separator}${testClassName}.java")
            }
        } catch (t: Throwable) {
            logger.error { "An error has occurred while generating test for snippet $targetClassFqn : $t" }
            throw t
        } finally {
            val duration = ChronoUnit.MILLIS.between(started, now())
            logger.debug { "Generating test for [$targetClassFqn] - completed in [$duration] (ms)" }
        }
    }

}