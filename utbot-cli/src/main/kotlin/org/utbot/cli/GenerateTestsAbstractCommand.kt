package org.utbot.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import mu.KotlinLogging
import org.utbot.common.PathUtil.classFqnToPath
import org.utbot.common.PathUtil.replaceSeparator
import org.utbot.common.PathUtil.toPath
import org.utbot.common.PathUtil.toURL
import org.utbot.common.toPath
import org.utbot.engine.Mocker
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.MockitoStaticMocking
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.testFrameworkByName
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TreatOverflowAsError
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.summary.summarize
import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

private const val LONG_GENERATION_TIMEOUT = 1_200_000L

private val logger = KotlinLogging.logger {}

abstract class GenerateTestsAbstractCommand(name: String, help: String) :
    CliktCommand(name = name, help = help) {

    abstract val classPath:String?

    private val mockStrategy by option("-m", "--mock-strategy", help = "Defines the mock strategy")
        .choice(
            "do-not-mock" to MockStrategyApi.NO_MOCKS,
            "package-based" to MockStrategyApi.OTHER_PACKAGES,
            "all-except-cut" to MockStrategyApi.OTHER_CLASSES
        )
        .default(MockStrategyApi.NO_MOCKS)

    private val testFramework by option("--test-framework", help = "Test framework to be used")
        .choice("junit4", "junit5", "testng")
        .default("junit4")

    private val staticsMocking by option(
        "--mock-statics",
        help = "Choose framework for mocking statics (or not mock statics at all"
    )
        .choice(
            "do-not-mock-statics" to NoStaticMocking,
            "mock-statics" to MockitoStaticMocking,
        )
        .default(StaticsMocking.defaultItem)

    private val forceStaticMocking by option(
        "-f",
        "--force-static-mocking",
        help = "Forces mocking static methods and constructors for \"--mock-always\" classes"
    )
        .choice(
            "force" to ForceStaticMocking.FORCE,
            "do-not-force" to ForceStaticMocking.DO_NOT_FORCE
        )
        .default(ForceStaticMocking.defaultItem)

    private val treatOverflowAsError by option(
        "--overflow",
        help = "Treat overflows as errors"
    )
        .choice(
            "ignore" to TreatOverflowAsError.IGNORE,
            "error" to TreatOverflowAsError.AS_ERROR,
        )
        .default(TreatOverflowAsError.defaultItem)

    protected val classesToMockAlways by option(
        "--mock-always",
        "--ma",
        help = "Classes fully qualified name to force mocking theirs static methods and constructors " +
                "(you can use it multiple times to provide few classes);" +
                "default classes = ${
                    Mocker.defaultSuperClassesToMockAlwaysNames.joinToString(
                        separator = newline() + "\t",
                        prefix = newline() + "\t",
                    )
                }"
    )
        .multiple()
        .unique()
        // TODO maybe better way exists
        .check("Some classes were not found") { fullyQualifiedNames ->
            for (fullyQualifiedName in fullyQualifiedNames) {
                try {
                    val initialize = false
                    Class.forName(fullyQualifiedName, initialize, ClassLoader.getSystemClassLoader())
                } catch (e: ClassNotFoundException) {
                    return@check false
                }
            }
            return@check true
        }

    abstract val codegenLanguage: CodegenLanguage

    private val generationTimeout by option(
        "--generation-timeout",
        help = "Specifies the maximum time in milliseconds used to generate tests ($LONG_GENERATION_TIMEOUT by default)"
    )
        .long()
        .default(LONG_GENERATION_TIMEOUT)

    protected open val classLoader: URLClassLoader by lazy {
        val urls = classPath!!
            .split(File.pathSeparator)
            .map { uri ->
                uri.toPath().toURL()
            }
            .toTypedArray()
        URLClassLoader(urls)
    }

    abstract override fun run()

    protected fun getWorkingDirectory(classFqn: String): Path? {
        val classRelativePath = classFqnToPath(classFqn) + ".class"
        val classAbsoluteURL = classLoader.getResource(classRelativePath) ?: return null
        val classAbsolutePath = replaceSeparator(classAbsoluteURL.toPath().toString())
            .removeSuffix(classRelativePath)
        return Paths.get(classAbsolutePath)
    }

    protected fun loadClassBySpecifiedFqn(classFqn: String): KClass<*> =
        classLoader.loadClass(classFqn).kotlin

    protected fun generateTestSets(
        targetMethods: List<UtMethod<*>>,
        sourceCodeFile: Path? = null,
        searchDirectory: Path,
        chosenClassesToMockAlways: Set<ClassId>
    ): List<UtMethodTestSet> =
        TestCaseGenerator.generate(
            targetMethods,
            mockStrategy,
            chosenClassesToMockAlways,
            generationTimeout
        ).map {
            if (sourceCodeFile != null) it.summarize(sourceCodeFile.toFile(), searchDirectory) else it
        }


    protected fun withLogger(targetClassFqn: String, block: Runnable) {
        val started = now()
        try {
            logger.debug { "Generating test for [$targetClassFqn] - started" }
            logger.debug { "Classpath to be used: ${newline()} $classPath ${newline()}" }
            block.run()
        } catch (t: Throwable) {
            logger.error { "An error has occurred while generating test for snippet $targetClassFqn : $t" }
            throw t
        } finally {
            val duration = ChronoUnit.MILLIS.between(started, now())
            logger.debug { "Generating test for [$targetClassFqn] - completed in [$duration] (ms)" }
        }
    }

    protected fun generateTest(classUnderTest: KClass<*>, testClassname: String, testSets: List<UtMethodTestSet>): String =
        initializeCodeGenerator(
            testFramework,
            classUnderTest
        ).generateAsString(testSets, testClassname)

    protected fun initializeEngine(workingDirectory: Path) {
        val classPathNormalized =
            classLoader.urLs.joinToString(separator = File.pathSeparator) { it.toPath().absolutePath }

        // TODO: SAT-1566
        // Set UtSettings parameters.
        UtSettings.treatOverflowAsError = treatOverflowAsError == TreatOverflowAsError.AS_ERROR

        TestCaseGenerator.init(workingDirectory, classPathNormalized, System.getProperty("java.class.path"))
    }

    private fun initializeCodeGenerator(testFramework: String, classUnderTest: KClass<*>): CodeGenerator {
        val generateWarningsForStaticMocking =
            forceStaticMocking == ForceStaticMocking.FORCE && staticsMocking is NoStaticMocking
        return CodeGenerator().apply {
            init(
                testFramework = testFrameworkByName(testFramework),
                classUnderTest = classUnderTest.java,
                codegenLanguage = codegenLanguage,
                staticsMocking = staticsMocking,
                forceStaticMocking = forceStaticMocking,
                generateWarningsForStaticMocking = generateWarningsForStaticMocking,
            )
        }
    }

    protected fun KClass<*>.targetMethods() =
        this.java.declaredMethods.mapNotNull {
            toUtMethod(it, kClass = this)
        }

    private fun toUtMethod(method: Method, kClass: KClass<*>): UtMethod<*>? =
        method.kotlinFunction?.let {
            UtMethod(it as KCallable<*>, kClass)
        } ?: run {
            logger.info("Method does not have a kotlin function: $method")
            null
        }

    protected fun saveToFile(snippet: String, outputPath: String?) =
        outputPath?.let {
            Files.write(it.toPath(), listOf(snippet))
        }

    protected fun now(): LocalDateTime = LocalDateTime.now()

    protected fun newline(): String = System.lineSeparator()
}