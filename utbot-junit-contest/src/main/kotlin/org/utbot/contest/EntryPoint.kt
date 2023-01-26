package org.utbot.contest

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.utbot.framework.SummariesGenerationType
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.Settings
import java.io.File
import java.net.URLClassLoader

/**
 * Use this key as jvm property to set a different test generation tool.
 *
 * For example, `-Dutbot.entryPointTool=org.utbot.contest.ConsoleInputEntryPoint`.
 *
 * @see ConsoleInputEntryPointTool
 */
const val entryPointToolKey = "utbot.entryPointTool"

private val oneClassEntryPoint = EntryPointTool {
    sequence {
        yield(
            Data(
                name = "example",
                classPaths = listOf(File("utbot-sample/build/classes/java/main").absoluteFile),
                classUnderTest = "guava.examples.math.IntMath",
                outputDirectory = File("utbot-junit-contest/build/output").absoluteFile,
                timeBudget = 60,
                fuzzingRatio = 0.1,
                methodNameFilter = "pow"
            )
        )
    }
}

fun main(args: Array<String>) {
    val entryPointClass = System.getProperty(entryPointToolKey)
    val entryPoint: EntryPointTool = if (entryPointClass != null) {
        val constructors = EntryPointTool::class.java.classLoader.loadClass(entryPointClass).constructors
        val emptyConstructor = constructors.find { it.parameters.isEmpty() }
        val argConstructor = constructors.find {
            if (it.parameters.size != 1) {
                false
            } else {
                val type = it.parameters[0].type
                type == Array<String>::class.java
            }
        }
        when {
            argConstructor != null -> argConstructor.newInstance(args)
            emptyConstructor != null -> emptyConstructor.newInstance()
            else -> error("${entryPointClass::class} doesn't have proper constructor")
        } as EntryPointTool
    } else {
        oneClassEntryPoint
    }

    runBlocking {
        entryPoint.setup()
        entryPoint.prepare().forEach { data ->
            val urls = data.classPaths.map { it.toURI().toURL() }.toTypedArray()
            val loader = URLClassLoader(urls)
            withTimeout(data.timeBudget) {
                withUtContext(ContextManager.createNewContext(loader)) {
                    entryPoint.run(data, loader)
                }
            }
        }
        entryPoint.finalize()
    }
}

/**
 * Data contains all information for test generation of the one class under test (CUT).
 */
class Data(
    /**
     * Common name, for example, it can refer to a project (like, guava or spoon).
     */
    val name: String,
    /**
     * All class paths for correct run.
     */
    val classPaths: List<File>,
    /**
     * FQN of the class under test.
     */
    val classUnderTest: String,
    /**
     * Optional method filter.
     */
    val methodNameFilter: String? = null,
    /**
     * Output directory for generated tests.
     */
    val outputDirectory: File,
    /**
     * Maximum time for running.
     */
    val timeBudget: Long,
    val fuzzingRatio: Double,
)

/**
 * Interface declares the minimum steps to run test generation.
 *
 * Every test generation contains the next steps:
 * 1. Setting the environment of a particular tool.
 * 2. Preparing data for one class under test (CUT) run.
 * 3. Run generation for one class under test.
 * 4. After all CUTs are done the [finalize] method is called.
 *
 * Every step is suspendable. Also, [prepare] method returns a sequence
 * that can be implemented using [sequence] call, that also can be suspended.
 * Thus, here are 2 types of data submission:
 * 1. Already prepared and read data that use simple [sequenceOf] call.
 * 2. Lazy-loading or user data that can be submitted with delay (waiting for user input, for example).
 *
 * To use concrete implementation of this interface using [entryPointToolKey] from jvm option
 * when running the jar, the implementation must have empty public constructor or constructor,
 * that accepts array of string with command line arguments.
 *
 * Defaults implementations of [setup] and [run] are already working without any other tuning (see [oneClassEntryPoint]).
 */
fun interface EntryPointTool {

    /**
     * Setup is called before [prepare] method is called.
     *
     * Can be used to change some global or local states for future test generation.
     */
    suspend fun setup() {
        Settings.defaultConcreteExecutorPoolSize = 1
        UtSettings.useFuzzing = true
        UtSettings.classfilesCanChange = false
        // We need to use assemble model generator to increase readability
        UtSettings.useAssembleModelGenerator = true
        UtSettings.summaryGenerationType = SummariesGenerationType.LIGHT
        UtSettings.preferredCexOption = false
        UtSettings.warmupConcreteExecution = true
        UtSettings.testMinimizationStrategyType = TestSelectionStrategyType.COVERAGE_STRATEGY
        UtSettings.ignoreStringLiterals = true
        UtSettings.maximizeCoverageUsingReflection = true
        UtSettings.useSandbox = false
    }

    /**
     * Prepares and submits data for running.
     *
     * Can use simple sequences or lazy, using [sequence]. The latest is implemented in the [ConsoleInputEntryPointTool].
     */
    suspend fun prepare(): Sequence<Data>

    /**
     * Run test generation for particular data.
     *
     * This job is canceled after [Data.timeBudget] ms is over.
     * If any long task should be done before the run use [prepare] method with lazy [sequence]
     * to submit new tasks.
     */
    suspend fun run(data: Data, loader: ClassLoader) {
        @Suppress("OPT_IN_USAGE")
        runGeneration(
            data.name,
            ClassUnderTest(loader.loadClass(data.classUnderTest).id, data.outputDirectory),
            data.timeBudget,
            data.fuzzingRatio,
            data.classPaths.joinToString(System.getProperty("path.separator")),
            false,
            data.methodNameFilter
        )
    }

    /**
     * Finalize is called when all data from [prepare] are processed.
     */
    suspend fun finalize() {
        ContextManager.cancelAll()
        ConcreteExecutor.defaultPool.close()
    }
}

/**
 * Example of dynamically loaded and attached service.
 */
@Suppress("unused")
class ConsoleInputEntryPointTool(args: Array<String>) : EntryPointTool {

    init {
        println("Args: [${args.joinToString()}]")
    }

    override suspend fun prepare() = sequence {
        while (true) {
            println("Enter classpath: ")
            val classPath = readln().takeIf(String::isNotBlank) ?: break
            println("Enter class under test: ")
            val classUnderTest = readln().takeIf(String::isNotBlank) ?: break
            println("Enter output directory: ")
            val outputDirectory = readln().takeIf(String::isNotBlank) ?: break
            println("Enter time budget: ")
            val timeBudget = readln().takeIf(String::isNotBlank)?.toLong() ?: break
            println("Enter fuzzing ratio: ")
            val fuzzingRatio = readln().takeIf(String::isNotBlank)?.toDouble() ?: break
            yield(
                Data(
                    "console",
                    listOf(File(classPath)),
                    classUnderTest,
                    null,
                    File(outputDirectory),
                    timeBudget,
                    fuzzingRatio
                )
            )
        }
    }

    override suspend fun run(data: Data, loader: ClassLoader) {
        println("Test generation started")
        super.run(data, loader)
        println("Test generation finished")
    }
}