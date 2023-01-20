package org.utbot.contest

import mu.KotlinLogging
import org.objectweb.asm.Type
import org.utbot.common.FileUtil
import org.utbot.common.bracket
import org.utbot.common.filterWhen
import org.utbot.common.info
import org.utbot.common.isAbstract
import org.utbot.engine.EngineController
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.junitByVersion
import org.utbot.framework.codegen.CodeGenerator
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.util.isKnownImplicitlyDeclaredMethod
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.ConcreteExecutorPool
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.warmup.Warmup
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KCallable
import kotlin.reflect.jvm.isAccessible
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.utbot.framework.SummariesGenerationType
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.util.isSynthetic
import org.utbot.framework.util.jimpleBody
import org.utbot.summary.summarize

internal const val junitVersion = 4
private val logger = KotlinLogging.logger {}

@Suppress("unused")
object Contest

enum class ContestMessage {
    INIT,
    READY,
    RUN;

    override fun toString(): String {
        return "[${super.toString()}]"
    }

}

val charset = Charsets.UTF_8

val generatedLanguage: CodegenLanguage = CodegenLanguage.JAVA
val mockStrategyApi: MockStrategyApi = mockStrategyApiFromString(System.getProperty("utBotMockStrategyApi")) ?: MockStrategyApi.NO_MOCKS
val staticsMocking = StaticsMocking.defaultItem
val forceStaticMocking: ForceStaticMocking = ForceStaticMocking.FORCE

val dependencyPath = System.getProperty("java.class.path")

fun mockStrategyApiFromString(value:String?):MockStrategyApi? = when(value) {
    "NO_MOCKS" -> MockStrategyApi.NO_MOCKS
    "OTHER_PACKAGES" -> MockStrategyApi.OTHER_PACKAGES
    "OTHER_CLASSES" -> MockStrategyApi.OTHER_CLASSES
    else -> null
}

@ObsoleteCoroutinesApi
fun main(args: Array<String>) {
    require(args.size == 3) {
        """Wrong arguments. Expected arguments: <classfileDir> <classpathString> <output directory for generated tests>
          |Actual arguments: ${args.joinToString()}
        """.trimMargin()
    }

    setOptions()

    val classfileDir = Paths.get(args[0])
    val classpathString = args[1]
    val outputDir = File(args[2])

    println("Started UtBot Contest, classfileDir = $classfileDir, classpathString=$classpathString, outputDir=$outputDir, mocks=$mockStrategyApi")

    val cpEntries = classpathString.split(File.pathSeparator).map { File(it) }
    val classLoader = URLClassLoader(cpEntries.map { it.toUrl() }.toTypedArray())
    val context = UtContext(classLoader)


    withUtContext(context) {
        // Initialize the soot before a contest is started.
        // This saves the time budget for real work instead of soot initialization.
        TestCaseGenerator(listOf(classfileDir), classpathString, dependencyPath, JdkInfoService.provide())

        logger.info().bracket("warmup: kotlin reflection :: init") {
            prepareClass(ConcreteExecutorPool::class.java, "")
            prepareClass(Warmup::class.java, "")
        }

        println("${ContestMessage.INIT}")

        while (true) {
            val line = readLine()
            println(">> $line")
            if (line == null) {
                return
            }
            val cmd = line.split(" ")
            if (cmd.isEmpty() || cmd[0] != "${ContestMessage.RUN}")
                continue

            val classUnderTestName = cmd[1]
            val timeBudgetSec = cmd[2].toLong()
            val cut = ClassUnderTest(classLoader.loadClass(classUnderTestName).id, outputDir, classfileDir.toFile())

            runGeneration(
                project = "Contest",
                cut,
                timeBudgetSec,
                fuzzingRatio = 0.1,
                classpathString,
                runFromEstimator = false,
                methodNameFilter = null
            )
            println("${ContestMessage.READY}")
        }
    }
    ConcreteExecutor.defaultPool.close()
}

fun setOptions() {
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


@ObsoleteCoroutinesApi
@SuppressWarnings
fun runGeneration(
    project: String,
    cut: ClassUnderTest,
    timeLimitSec: Long,
    fuzzingRatio: Double,
    classpathString: String,
    runFromEstimator: Boolean,
    methodNameFilter: String? = null // For debug purposes you can specify method name
): StatsForClass = runBlocking {
    val testsByMethod: MutableMap<ExecutableId, MutableList<UtExecution>> = mutableMapOf()
    val currentContext = utContext

    val timeBudgetMs = timeLimitSec * 1000
    val generationTimeout: Long = timeBudgetMs - timeBudgetMs * 15 / 100 // 4000 ms for terminate all activities and finalize code in file

    logger.debug { "-----------------------------------------------------------------------------" }
    logger.info(
        "Contest.runGeneration: Time budget: $timeBudgetMs ms, Generation timeout=$generationTimeout ms, " +
                "classpath=$classpathString, methodNameFilter=$methodNameFilter"
    )

    if (runFromEstimator) {
        setOptions()
        //will not be executed in real contest
        logger.info().bracket("warmup: 1st optional soot initialization and executor warmup (not to be counted in time budget)") {
            TestCaseGenerator(listOf(cut.classfileDir.toPath()), classpathString, dependencyPath, JdkInfoService.provide(), forceSootReload = false)
        }
        logger.info().bracket("warmup (first): kotlin reflection :: init") {
            prepareClass(ConcreteExecutorPool::class.java, "")
            prepareClass(Warmup::class.java, "")
        }
    }

    //remaining budget
    val startTime = System.currentTimeMillis()
    fun remainingBudget() = max(0, generationTimeout - (System.currentTimeMillis() - startTime))

    logger.info("$cut")

    if (cut.classLoader.javaClass != URLClassLoader::class.java) {
        logger.error("Seems like classloader for cut not valid (maybe it was backported to system): ${cut.classLoader}")
    }

    val statsForClass = StatsForClass(project, cut.fqn)

    val codeGenerator = CodeGenerator(
            cut.classId,
            testFramework = junitByVersion(junitVersion),
            staticsMocking = staticsMocking,
            forceStaticMocking = forceStaticMocking,
            generateWarningsForStaticMocking = false,
            cgLanguageAssistant = CgLanguageAssistant.getByCodegenLanguage(CodegenLanguage.defaultItem),
        )

    logger.info().bracket("class ${cut.fqn}", { statsForClass }) {

        val filteredMethods = logger.info().bracket("preparation class ${cut.clazz}: kotlin reflection :: run") {
            prepareClass(cut.clazz, methodNameFilter)
        }

        statsForClass.methodsCount = filteredMethods.size

        // nothing to process further
        if (filteredMethods.isEmpty()) return@runBlocking statsForClass

        val testCaseGenerator =
            logger.info().bracket("2nd optional soot initialization") {
                TestCaseGenerator(listOf(cut.classfileDir.toPath()), classpathString, dependencyPath, JdkInfoService.provide(), forceSootReload = false)
            }


        val engineJob = CoroutineScope(SupervisorJob() + newSingleThreadContext("SymbolicExecution") + currentContext ).launch {

            if (remainingBudget() == 0L) {
                logger.warn {"No time left for processing class"}
                return@launch
            }

            var remainingMethodsCount = filteredMethods.size
            val method2controller = filteredMethods.associateWith { EngineController() }

            for ((method, controller) in method2controller) {
                val job = launch(currentContext) {
                    val methodJob = currentCoroutineContext().job

                    logger.debug { " ... " }
                    val statsForMethod = StatsForMethod("${method.classId.simpleName}#${method.name}")
                    statsForClass.statsForMethods.add(statsForMethod)


                    if (!isActive) {
                        logger.warn { "Stop processing methods of [${cut.fqn}] because job has been canceled" }
                        return@launch
                    }


                    val minSolverTimeout = 10L
                    val remainingBudget = remainingBudget()
                    if (remainingBudget < minSolverTimeout*2 ) {
                        logger.warn { "No time left for [${cut.fqn}]" }
                        return@launch
                    }

                    val budgetForMethod = remainingBudget / remainingMethodsCount

                    val solverTimeout = min(1000L, max(minSolverTimeout /* 0 means solver have no timeout*/, budgetForMethod / 2)).toInt()

                    // @todo change to the constructor parameter
                    UtSettings.checkSolverTimeoutMillis = solverTimeout

                    val budgetForLastSolverRequestAndConcreteExecutionRemainingStates = min(solverTimeout + 200L, budgetForMethod / 2)


                    val budgetForSymbolicExecution = max(0, budgetForMethod - budgetForLastSolverRequestAndConcreteExecutionRemainingStates)

                    UtSettings.utBotGenerationTimeoutInMillis = budgetForMethod
                    UtSettings.fuzzingTimeoutInMillis = (budgetForMethod * fuzzingRatio).toLong()

                    //start controller that will activate symbolic execution
                    GlobalScope.launch(currentContext) {
                        delay(budgetForSymbolicExecution)

                        if (methodJob.isActive) {
                            logger.info { "|> Starting concrete execution for remaining state: $method" }
                            controller.executeConcretely = true
                        }

                        delay(budgetForLastSolverRequestAndConcreteExecutionRemainingStates)
                        if (methodJob.isActive) {
                            logger.info { "(X) Cancelling concrete execution: $method" }
                            methodJob.cancel()
                        }
                    }


                    var testsCounter = 0
                    logger.info().bracket("method $method", { statsForMethod }) {
                        logger.info {
                            " -- Remaining time budget: $remainingBudget ms, " +
                                    "#remaining_methods: $remainingMethodsCount, " +
                                    "budget for method: $budgetForMethod ms, " +
                                    "solver timeout: $solverTimeout ms, " +
                                    "budget for symbolic execution: $budgetForSymbolicExecution ms, " +
                                    "budget for concrete execution: $budgetForLastSolverRequestAndConcreteExecutionRemainingStates ms, " +
                                    " -- "

                        }

                        testCaseGenerator.generateAsync(controller, method, mockStrategyApi)
                            .collect { result ->
                                when (result) {
                                    is UtExecution -> {
                                        try {
                                            val testMethodName = testMethodName(method.toString(), ++testsCounter)
                                            val className = Type.getInternalName(method.classId.jClass)
                                            logger.debug { "--new testCase collected, to generate: $testMethodName" }
                                            statsForMethod.testsGeneratedCount++
                                            result.coverage?.let {
                                                statsForClass.updateCoverage(
                                                    newCoverage = it,
                                                    isNewClass = !statsForClass.testedClassNames.contains(className),
                                                    fromFuzzing = result is UtFuzzedExecution
                                                )
                                            }
                                            statsForClass.testedClassNames.add(className)

                                            testsByMethod.getOrPut(method) { mutableListOf() } += result
                                        } catch (e: Throwable) {
                                            //Here we need isolation
                                            logger.error(e) { "Code generation failed" }
                                        }
                                    }
                                    is UtError -> {
                                        if (statsForMethod.failReasons.add(FailReason(result)))
                                            logger.error(result.error) { "Symbolic execution FAILED" }
                                        else
                                            logger.error { "Symbolic execution FAILED ... <<stack trace duplicated>>" }
                                    }
                                }
                            }

                        //hack
                        if (statsForMethod.isSuspicious && (ConcreteExecutor.lastSendTimeMs - ConcreteExecutor.lastReceiveTimeMs) > 5000) {
                            logger.error { "HEURISTICS: close instrumented process, because it haven't responded for long time: ${ConcreteExecutor.lastSendTimeMs - ConcreteExecutor.lastReceiveTimeMs}" }
                            ConcreteExecutor.defaultPool.close()
                        }
                    }
                }
                controller.job = job

                //don't start other methods while last method still in progress
                try {
                    job.join()
                } catch (t: Throwable) {
                    logger.error(t) { "Internal job error" }
                }

                remainingMethodsCount--
            }
        }


        val cancellator = GlobalScope.launch(currentContext) {
            delay(remainingBudget())
            if (engineJob.isActive) {
                logger.warn { "Cancelling job because timeout $generationTimeout ms elapsed (real cancellation can take time)" }
                statsForClass.canceledByTimeout = true
                engineJob.cancel()
            }
        }

        withTimeoutOrNull(remainingBudget() + 200 /* Give job some time to finish gracefully */) {
            try {
                engineJob.join()
            } catch (_: CancellationException) {
            } catch (e: Exception) { // need isolation because we want to write tests for class in any case
                logger.error(e) { "Error in engine invocation on class [${cut.fqn}]" }
            }
        }
        cancellator.cancel()

        val testSets = testsByMethod.map { (method, executions) ->
            UtMethodTestSet(method, minimizeExecutions(executions), jimpleBody(method))
                .summarize(sourceFile = null, cut.classfileDir.toPath())
        }

        logger.info().bracket("Flushing tests for [${cut.simpleName}] on disk") {
            writeTestClass(cut, codeGenerator.generateAsString(testSets))
        }
        //write classes
    }

    statsForClass
}

private fun prepareClass(javaClazz: Class<*>, methodNameFilter: String?): List<ExecutableId> {
    //1. all methods from cut
    val methods = javaClazz.declaredMethods
        .filterNot { it.isAbstract }
        .filterNotNull()

    //2. all constructors from cut
    val constructors =
        if (javaClazz.isAbstract || javaClazz.isEnum) emptyList() else javaClazz.declaredConstructors.filterNotNull()

    //3. Now join methods and constructors together
    val methodsToGenerate = methods.filter { it.isVisibleFromGeneratedTest } + constructors

    val classFilteredMethods = methodsToGenerate
        .map { it.executableId }
        .filter { methodNameFilter?.equals(it.name) ?: true }
        .filterWhen(UtSettings.skipTestGenerationForSyntheticAndImplicitlyDeclaredMethods) {
            !it.isSynthetic && !it.isKnownImplicitlyDeclaredMethod
        }
        .toList()


    return if (javaClazz.declaredClasses.isEmpty()) {
        classFilteredMethods
    } else {
        val nestedFilteredMethods = javaClazz.declaredClasses.flatMap { prepareClass(it, methodNameFilter) }
        classFilteredMethods + nestedFilteredMethods
    }
}

fun writeTestClass(cut: ClassUnderTest, testSetsAsString: String) {
    logger.info { "File size for ${cut.testClassSimpleName}: ${FileUtil.byteCountToDisplaySize(testSetsAsString.length.toLong())}" }
    cut.generatedTestFile.parentFile.mkdirs()
    cut.generatedTestFile.writeText(testSetsAsString, charset)
}

private inline fun <R> KCallable<*>.withAccessibility(block: () -> R): R {
    val prevAccessibility = isAccessible
    try {
        isAccessible = true
        return block()
    } finally {
        isAccessible = prevAccessibility
    }
}

@Suppress("DEPRECATION", "SameParameterValue")
private inline fun <T> runWithTimeout(timeout: Long, crossinline block: () -> T): T? {
    var value: T? = null
    val thread = thread {
        value = block() //highly depends that block doesn't throw exceptions
    }
    try {
        thread.join(timeout)

        if (thread.isAlive) {
            thread.interrupt()
            thread.join(50)
        }
        if (thread.isAlive)
            thread.stop()
    } catch (e: Exception) {
        logger.error(e) { "Can't run with timeout" }
    }
    return value
}

//val start = System.currentTimeMillis()
//internal fun currentTime(start: Long) = (System.currentTimeMillis() - start)

internal fun File.toUrl(): URL = toURI().toURL()

internal fun testMethodName(name: String, num: Int): String = "test${name.capitalize()}$num"

internal val Method.isVisibleFromGeneratedTest: Boolean
    get() = (this.modifiers and Modifier.ABSTRACT) == 0
            && (this.modifiers and Modifier.NATIVE) == 0

private fun StatsForClass.updateCoverage(newCoverage: Coverage, isNewClass: Boolean, fromFuzzing: Boolean) {
    coverage.update(newCoverage, isNewClass)
    // other coverage type updates by empty coverage to respect new class
    val emptyCoverage = newCoverage.copy(
        coveredInstructions = emptyList()
    )
    if (fromFuzzing) {
        fuzzedCoverage to concolicCoverage
    } else {
        concolicCoverage to fuzzedCoverage
    }.let { (targetSource, otherSource) ->
        targetSource.update(newCoverage, isNewClass)
        otherSource.update(emptyCoverage, isNewClass)
    }
}

private fun CoverageInstructionsSet.update(newCoverage: Coverage, isNewClass: Boolean) {
    if (isNewClass) {
        newCoverage.instructionsCount?.let {
            totalInstructions += it
        }
    }
    coveredInstructions.addAll(newCoverage.coveredInstructions)
}
