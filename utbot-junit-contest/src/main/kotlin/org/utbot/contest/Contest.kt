package org.utbot.contest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.utbot.common.bracket
import org.utbot.common.info
import org.utbot.engine.EngineController
import org.utbot.engine.isConstructor
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.junitByVersion
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
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
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import org.utbot.common.filterWhen
import org.utbot.framework.util.isKnownSyntheticMethod

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
        logger.info().bracket("warmup: kotlin reflection :: init") {
            prepareClass(ConcreteExecutorPool::class, "")
            prepareClass(Warmup::class, "")
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

            runGeneration(cut, timeBudgetSec, classpathString, runFromEstimator = false, methodNameFilter = null)
            println("${ContestMessage.READY}")
        }
    }
    ConcreteExecutor.defaultPool.close()
}

fun setOptions() {
    Settings.defaultConcreteExecutorPoolSize = 1
    UtSettings.useFuzzing = true
    UtSettings.classfilesCanChange = false
    UtSettings.useAssembleModelGenerator = false
    UtSettings.enableMachineLearningModule = false
    UtSettings.preferredCexOption = false
    UtSettings.warmupConcreteExecution = true
    UtSettings.testMinimizationStrategyType = TestSelectionStrategyType.COVERAGE_STRATEGY
    UtSettings.ignoreStringLiterals = true
    UtSettings.maximizeCoverageUsingReflection = true
}


@ObsoleteCoroutinesApi
@SuppressWarnings
fun runGeneration(
    cut: ClassUnderTest,
    timeLimitSec: Long,
    classpathString: String,
    runFromEstimator: Boolean,
    methodNameFilter: String? = null // For debug purposes you can specify method name
): StatsForClass = runBlocking {



    val testSets: MutableList<UtMethodTestSet> = mutableListOf()
    val currentContext = utContext

    val timeBudgetMs = timeLimitSec * 1000
    val generationTimeout: Long = timeBudgetMs - timeBudgetMs*15/100 // 4000 ms for terminate all activities and finalize code in file

    logger.debug { "-----------------------------------------------------------------------------" }
    logger.info(
        "Contest.runGeneration: Time budget: $timeBudgetMs ms, Generation timeout=$generationTimeout ms, " +
                "classpath=$classpathString, methodNameFilter=$methodNameFilter"
    )

    if (runFromEstimator) {
        setOptions()
        //will not be executed in real contest
        logger.info().bracket("warmup: 1st optional soot initialization and executor warmup (not to be counted in time budget)") {
            TestCaseGenerator(cut.classfileDir.toPath(), classpathString, dependencyPath)
        }
        logger.info().bracket("warmup (first): kotlin reflection :: init") {
            prepareClass(ConcreteExecutorPool::class, "")
            prepareClass(Warmup::class, "")
        }
    }

    //remaining budget
    val startTime = System.currentTimeMillis()
    fun remainingBudget() = max(0, generationTimeout - (System.currentTimeMillis() - startTime))

    logger.info("$cut")

    if (cut.classLoader.javaClass != URLClassLoader::class.java) {
        logger.error("Seems like classloader for cut not valid (maybe it was backported to system): ${cut.classLoader}")
    }

    val statsForClass = StatsForClass()

    val codeGenerator = CodeGenerator(
            cut.classId.jClass,
            testFramework = junitByVersion(junitVersion),
            staticsMocking = staticsMocking,
            forceStaticMocking = forceStaticMocking,
            generateWarningsForStaticMocking = false
        )

    // Doesn't work
/*    val concreteExecutorForCoverage =
        if (estimateCoverage)
            ConcreteExecutor(
                instrumentation = CoverageInstrumentation,
                pathsToUserClasses = cut.classfileDir.toPath().toString(),
                pathsToDependencyClasses = System.getProperty("java.class.path")
            ).apply {
                setKryoClassLoader(utContext.classLoader)
            }
        else null*/


    logger.info().bracket("class ${cut.fqn}", { statsForClass }) {

        val filteredMethods = logger.info().bracket("preparation class ${cut.kotlinClass}: kotlin reflection :: run") {
            prepareClass(cut.kotlinClass, methodNameFilter)
        }

        statsForClass.methodsCount = filteredMethods.size

        // nothing to process further
        if (filteredMethods.isEmpty()) return@runBlocking statsForClass

        val testCaseGenerator =
            logger.info().bracket("2nd optional soot initialization") {
                TestCaseGenerator(cut.classfileDir.toPath(), classpathString, dependencyPath)
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
                    val statsForMethod = StatsForMethod("${method.clazz.simpleName}#${method.callable.name}")
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


                    //start controller that will activate symbolic execution
                    GlobalScope.launch {
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
                                            logger.debug { "--new testCase collected, to generate: $testMethodName" }
                                            statsForMethod.testsGeneratedCount++

                                            //TODO: it is a strange hack to create fake test case for one [UtResult]
                                            testSets.add(UtMethodTestSet(method, listOf(result)))
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
                            logger.error { "HEURISTICS: close child process, because it haven't responded for long time: ${ConcreteExecutor.lastSendTimeMs - ConcreteExecutor.lastReceiveTimeMs}" }
                            ConcreteExecutor.defaultPool.close()
                        }
                    }
                }
                controller.job = job

                //don't start other methods while last method still in progress
                while (job.isActive)
                    yield()

                remainingMethodsCount--
            }
        }


        val cancellator = GlobalScope.launch {
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
            } catch (e: CancellationException) {
            } catch (e: Exception) { // need isolation because we want to write tests for class in any case
                logger.error(e) { "Error in engine invocation on class [${cut.fqn}]" }
            }
        }
        cancellator.cancel()

        logger.info().bracket("Flushing tests for [${cut.simpleName}] on disk") {
            writeTestClass(cut, codeGenerator.generateAsString(testSets))
        }
        //write classes
    }


    //Optional step that doesn't run in actual contest
    // Doesn't work
/*
    if (concreteExecutorForCoverage != null) {
        logger.info().bracket("Estimating coverage") {
            require(estimateCoverage)
            try {
                for ((name, testCase) in testCases) {
                    logger.debug {"-> estimate for $name" }
                    concreteExecutorForCoverage.executeTestCase(testCase.toValueTestCase())
                }
                statsForClass.coverage = concreteExecutorForCoverage.collectCoverage(cut.classId.jClass)
            } catch (e: Throwable) {
                logger.error(e) { "Error during coverage estimation" }
            }
        }
    }
*/


    statsForClass
}

private fun prepareClass(kotlinClass: KClass<*>, methodNameFilter: String?): List<UtMethod<*>> {
    //1. all methods and properties from cut
    val kotlin2java = kotlinClass.declaredMembers
        .filterNot { it.isAbstract }
        .map {
            it to when (it) {
                is KFunction -> it.javaMethod
                is KProperty -> it.javaGetter
                else -> null
            }
        }

    //2. all constructors from cut
    val kotlin2javaCtors =
        if (kotlinClass.isAbstract) emptyList() else kotlinClass.constructors.map { it to it.javaConstructor }

    //3. Now join properties, methods and constructors together
    val methodsToGenerate = kotlin2java
        //filter out abstract and native methods
        .filter { (_, javaMethod) -> javaMethod?.isVisibleFromGeneratedTest == true }
        //join
        .union(kotlin2javaCtors)

    val classFilteredMethods = methodsToGenerate.asSequence()
        .map { UtMethod(it.first, kotlinClass) }
        .filter { methodNameFilter?.equals(it.callable.name) ?: true }
        .filterNot { it.isConstructor && (it.clazz.isAbstract || it.clazz.java.isEnum) }
        .filterWhen(UtSettings.skipTestGenerationForSyntheticMethods) { !isKnownSyntheticMethod(it) }
        .toList()

    return if (kotlinClass.nestedClasses.isEmpty()) {
        classFilteredMethods
    } else {
        val nestedFilteredMethods = kotlinClass.nestedClasses.flatMap { prepareClass(it, methodNameFilter) }
        classFilteredMethods + nestedFilteredMethods
    }
}

fun writeTestClass(cut: ClassUnderTest, testSetsAsString: String) {
    logger.info { "File size for ${cut.testClassSimpleName}: ${FileUtils.byteCountToDisplaySize(testSetsAsString.length.toLong())}" }
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