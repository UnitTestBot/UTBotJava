package org.utbot.contest

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.analytics.Predictors
import org.utbot.features.FeatureExtractorFactoryImpl
import org.utbot.features.FeatureProcessorWithStatesRepetitionFactory
import org.utbot.framework.PathSelectorType
import org.utbot.framework.SummariesGenerationType
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.Settings
import org.utbot.predictors.MLPredictorFactoryImpl
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
                classPaths = listOf("utbot-sample/build/classes/java/main"),
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
    @Suppress("UNCHECKED_CAST")
    val entryPoint: EntryPointTool<Data> = if (entryPointClass != null) {
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
        } as EntryPointTool<Data>
    } else {
        oneClassEntryPoint
    }

    runBlocking {
        entryPoint.setup()
        entryPoint.prepare().forEach { data ->
            withTimeout(data.timeBudget) {
                withUtContext(ContextManager.createNewContext(data.classLoader)) {
                    entryPoint.run(data)
                }
            }
        }
        entryPoint.finalize()
    }
}

/**
 * Data contains all information for test generation of the one class under test (CUT).
 */
open class Data(
    /**
     * Common name, for example, it can refer to a project (like, guava or spoon).
     */
    val name: String,
    /**
     * All class paths for correct run.
     */
    val classPaths: List<String>,
    /**
     * Used classloader
     */
    val classLoader: ClassLoader = classPaths.map { File(it).toURI().toURL() }.toTypedArray().let(::URLClassLoader),
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
    /**
     * Fuzzing ratio
     */
    val fuzzingRatio: Double = 0.1,
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
fun interface EntryPointTool<D : Data> {

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
    suspend fun prepare(): Sequence<D>

    /**
     * Run test generation for particular data.
     *
     * This job is canceled after [Data.timeBudget] ms is over.
     * If any long task should be done before the run use [prepare] method with lazy [sequence]
     * to submit new tasks.
     */
    suspend fun run(data: D) {
        @Suppress("OPT_IN_USAGE")
        runGeneration(
            data.name,
            ClassUnderTest(data.classLoader.loadClass(data.classUnderTest).id, data.outputDirectory),
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
 * EXPERIMENTAL copy of [ContestEstimator].
 */
@Suppress("unused")
class ContestEstimatorEntryPointTool : EntryPointTool<ContestEstimatorEntryPointTool.DataWithTool> {
    private val tools = listOf(Tool.UtBot)
    private val logger = KotlinLogging.logger {}
    private val globalStats = GlobalStats()

    class DataWithTool(
        name: String,
        classPaths: List<String>,
        classLoader: ClassLoader,
        classUnderTest: String,
        methodNameFilter: String? = null,
        outputDirectory: File,
        timeBudget: Long,
        fuzzingRatio: Double = 0.1,
        val tool: Tool,
        val project: ProjectToEstimate,
        val stats: StatsForProject
    ) : Data(name, classPaths, classLoader, classUnderTest, methodNameFilter, outputDirectory, timeBudget, fuzzingRatio)

    override suspend fun setup() {
        super.setup()
        JdkInfoService.jdkInfoProvider = ContestEstimatorJdkInfoProvider(javaHome)

        EngineAnalyticsContext.featureProcessorFactory = FeatureProcessorWithStatesRepetitionFactory()
        EngineAnalyticsContext.featureExtractorFactory = FeatureExtractorFactoryImpl()
        EngineAnalyticsContext.mlPredictorFactory = MLPredictorFactoryImpl()
        if (UtSettings.pathSelectorType == PathSelectorType.ML_SELECTOR || UtSettings.pathSelectorType == PathSelectorType.TORCH_SELECTOR) {
            Predictors.stateRewardPredictor = EngineAnalyticsContext.mlPredictorFactory()
        }
    }

    override suspend fun prepare(): Sequence<DataWithTool> {
        val classesLists = File(Paths.classesLists)
        val classpathDir = File(Paths.jarsDir)
        val outputDir = File(Paths.outputDir)
        val timeLimit = 120L
        val fuzzingRatio = 0.1
        @Suppress("RedundantNullableReturnType")
        val methodFilter: String? = "com.google.common.primitives.Shorts.*"
        @Suppress("RedundantNullableReturnType")
        val projectFilter: List<String>? = listOf("guava-26.0")
        val processedClassesThreshold = 9999

        val testCandidatesDir = File(outputDir, "test_candidates")
        val compiledTestDir = File(outputDir, "compiled")
        compiledTestDir.mkdirs()
        val unzippedJars = File(outputDir, "unzipped")

        // fix for CTRL-ALT-SHIFT-C from IDEA, which copies in class#method form
        // fix for path form
        val updatedMethodFilter = methodFilter
            ?.replace('#', '.')
            ?.replace('/', '.')

        val classFqnFilter: String? = updatedMethodFilter?.substringBeforeLast('.')
        val methodNameFilter: String? = updatedMethodFilter?.substringAfterLast('.')?.let { if (it == "*") null else it }

        if (updatedMethodFilter != null)
            logger.info { "Filtering: class='$classFqnFilter', method ='$methodNameFilter'" }

        val projectToClassFQNs = classesLists.listFiles()!!.associate { it.name to File(it, "list").readLines() }

        val projects = mutableListOf<ProjectToEstimate>()

        logger.info { "Found ${projectToClassFQNs.size} projects" }

        for ((name, classesFQN) in projectToClassFQNs) {
            val project = ProjectToEstimate(
                name,
                classesFQN,
                File(classpathDir, name).listFiles()!!.filter { it.toString().endsWith("jar") },
                testCandidatesDir,
                unzippedJars
            )

            logger.info { "\n>>>" }
            logger.info { project }
            project.unzipConditionally()

            //smoke test
            project.classFQNs.forEach { fqn ->
                try {
                    project.classloader.loadClass(fqn).kotlin
                } catch (e: Throwable) {
                    logger.warn(e) { "Smoke test failed for class: $fqn" }
                }
            }

            projects.add(project)
        }

        return sequence {
            try {
                tools.forEach { tool ->
                    var classIndex = 0

                    outer@ for (project in projects) {
                        if (projectFilter != null && project.name !in projectFilter) continue

                        val statsForProject = StatsForProject(project.name)
                        globalStats.projectStats.add(statsForProject)

                        logger.info { "------------- project [${project.name}] ---- " }

                        // take all the classes from the corresponding jar if a list of the specified classes is empty
                        val extendedClassFqn = project.classFQNs.ifEmpty { project.classNames }

                        for (classFqn in extendedClassFqn.filter { classFqnFilter?.equals(it) ?: true }) {
                            classIndex++
                            if (classIndex > processedClassesThreshold) {
                                logger.info { "Reached limit of $processedClassesThreshold classes" }
                                break@outer
                            }

                            try {
                                val cut =
                                    ClassUnderTest(
                                        project.classloader.loadClass(classFqn).id,
                                        project.outputTestSrcFolder,
                                        project.unzippedDir
                                    )

                                logger.info { "------------- [${project.name}] ---->--- [$classIndex:$classFqn] ---------------------" }

                                yield(
                                    DataWithTool(
                                        name = project.name,
                                        classUnderTest = cut.fqn,
                                            classPaths = listOf(project.compileClasspathString),
                                        classLoader = project.classloader,
                                        methodNameFilter = methodNameFilter,
                                        outputDirectory = outputDir,
                                        timeBudget = timeLimit,
                                        fuzzingRatio = fuzzingRatio,
                                        tool = tool,
                                        project = project,
                                        stats = statsForProject
                                    )
                                )
                            }
                            catch (e: Throwable) {
                                logger.warn(e) { "===================== ERROR IN [${project.name}] FOR [$classIndex:$classFqn] ============" }
                            }
                        }
                    }
                }
            } finally {

            }
        }
    }

    override suspend fun run(data: DataWithTool) {
        val project = data.project
        data.tool.run(
            project = project,
            cut = ClassUnderTest(
                project.classloader.loadClass(data.classUnderTest).id,
                project.outputTestSrcFolder,
                project.unzippedDir
            ),
            data.timeBudget,
            data.fuzzingRatio,
            data.methodNameFilter,
            data.stats,
            data.outputDirectory,
            data.classUnderTest
        )
    }

    override suspend fun finalize() {
        logger.info { globalStats }
        super.finalize()
    }
}

/**
 * Example of dynamically loaded and attached service.
 */
@Suppress("unused")
class ConsoleInputEntryPointTool(args: Array<String>) : EntryPointTool<Data> {

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
                    name = "console",
                    classPaths = listOf(classPath),
                    classUnderTest = classUnderTest,
                    outputDirectory = File(outputDirectory),
                    timeBudget = timeBudget,
                    fuzzingRatio = fuzzingRatio
                )
            )
        }
    }

    override suspend fun run(data: Data) {
        println("Test generation started")
        super.run(data)
        println("Test generation finished")
    }
}