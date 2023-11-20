package org.utbot.contest.usvm

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.jacodb.api.ext.findClass
import org.jacodb.api.ext.jcdbSignature
import org.jacodb.api.ext.toType
import org.jacodb.approximation.Approximations
import org.jacodb.impl.features.InMemoryHierarchy
import org.objectweb.asm.Type
import org.usvm.UMachineOptions
import org.usvm.instrumentation.util.jcdbSignature
import org.usvm.machine.JcMachine
import org.usvm.machine.state.JcState
import org.utbot.common.ThreadBasedExecutor
import org.utbot.common.debug
import org.utbot.common.info
import org.utbot.common.measureTime
import org.utbot.contest.*
import org.utbot.contest.junitVersion
import org.utbot.contest.testMethodName
import org.utbot.contest.usvm.executor.JcTestExecutor
import org.utbot.contest.usvm.executor.UTestRunner
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.junitByVersion
import org.utbot.framework.codegen.generator.CodeGenerator
import org.utbot.framework.codegen.generator.CodeGeneratorParams
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.constructor
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.method
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.fuzzer.ReferencePreservingIntIdGenerator
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.summary.usvm.summarizeAll
import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.math.max

private val logger = KotlinLogging.logger {}

@ObsoleteCoroutinesApi
@SuppressWarnings
fun runUsvmGeneration(
    project: String,
    cut: ClassUnderTest,
    timeLimitSec: Long,
    fuzzingRatio: Double,
    classpathString: String,
    runFromEstimator: Boolean,
    expectedExceptions: ExpectedExceptionsForClass,
    methodNameFilter: String? = null // For debug purposes you can specify method name
): StatsForClass = runBlocking {
    val testsByMethod: MutableMap<ExecutableId, MutableList<UtExecution>> = mutableMapOf()

    val timeBudgetMs = timeLimitSec * 1000
    val generationTimeoutMillisWithoutCodegen: Long = timeBudgetMs - timeBudgetMs * 15 / 100 // 15% to terminate all activities and finalize code in file

    logger.debug { "-----------------------------------------------------------------------------" }
    logger.info(
        "Contest.runGeneration: Time budget: $timeBudgetMs ms, Generation timeout=$generationTimeoutMillisWithoutCodegen ms, " +
                "classpath=$classpathString, methodNameFilter=$methodNameFilter"
    )

    val classpathFiles = classpathString.split(File.pathSeparator).map { File(it) }

    val jcDbContainer by lazy {
        JacoDBContainer(
            key = classpathString,
            classpath = classpathFiles,
        ) {
            // TODO usvm-sbft: we may want to tune these JcSettings for contest
            useJavaRuntime(JdkInfoService.provide().path.toFile())
            installFeatures(InMemoryHierarchy, Approximations)
            loadByteCode(classpathFiles)
        }
    }

    val runner by lazy {
        if (!UTestRunner.isInitialized())
            UTestRunner.initRunner(classpathFiles.map { it.absolutePath }, jcDbContainer.cp)
        UTestRunner.runner
    }

    val resolver by lazy { JcTestExecutor(jcDbContainer.cp) }

    val idGenerator = ReferencePreservingIntIdGenerator()

    val instructionIds = mutableMapOf<Pair<String, Int>, Long>()
    val instructionIdProvider = InstructionIdProvider { methodSignature, instrIndex ->
        instructionIds.getOrPut(methodSignature to instrIndex) { instructionIds.size.toLong() }
    }

    if (runFromEstimator) {
        setOptions()
        //will not be executed in real contest
        logger.info().measureTime({ "warmup: 1st optional JacoDB initialization (not to be counted in time budget)" }) {
            jcDbContainer // force init lazy property
        }
        logger.info().measureTime({ "warmup: 1st optional executor start (not to be counted in time budget)" }) {
            runner.ensureRunnerAlive()
        }
    }

    //remaining budget
    val startTime = System.currentTimeMillis()
    logger.debug { "STARTED COUNTING BUDGET FOR ${cut.classId.name}" }
    fun remainingBudgetMillisWithoutCodegen() =
        max(0, generationTimeoutMillisWithoutCodegen - (System.currentTimeMillis() - startTime))

    logger.info("$cut")

    if (cut.classLoader.javaClass != URLClassLoader::class.java) {
        logger.error("Seems like classloader for cut not valid (maybe it was backported to system): ${cut.classLoader}")
    }

    val statsForClass = StatsForClass(project, cut.fqn)

    val codeGenerator = CodeGenerator(
        CodeGeneratorParams(
            cut.classId,
            projectType = ProjectType.PureJvm,
            testFramework = junitByVersion(junitVersion),
            staticsMocking = staticsMocking,
            forceStaticMocking = forceStaticMocking,
            generateWarningsForStaticMocking = false,
            cgLanguageAssistant = CgLanguageAssistant.getByCodegenLanguage(CodegenLanguage.defaultItem),
            runtimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.PASS,
        )
    )

    logger.info().measureTime({ "class ${cut.fqn}" }, { statsForClass }) {
        val filteredMethods = logger.info().measureTime({ "preparation class ${cut.clazz}: kotlin reflection :: run" }) {
            prepareClass(cut.clazz, methodNameFilter)
        }

        logger.info().measureTime({ "preparation: ensure JacoDB is initialized (counted in time budget)" }) {
            jcDbContainer // force init lazy property
        }
        logger.info().measureTime({ "preparation: ensure executor is started (counted in time budget)" }) {
            runner.ensureRunnerAlive()
        }

        // TODO usvm-sbft: better budget management
        val totalBudgetPerMethod = remainingBudgetMillisWithoutCodegen() / filteredMethods.size
        val concreteBudgetMsPerMethod = 500L
            .coerceIn((totalBudgetPerMethod / 10L).. (totalBudgetPerMethod / 2L))
        val symbolicBudgetPerMethod = totalBudgetPerMethod - concreteBudgetMsPerMethod
        logger.debug { "Symbolic budget per method: $symbolicBudgetPerMethod" }

        // TODO usvm-sbft: reuse same machine for different classes,
        //  right now I can't do that, because `timeoutMs` can't be changed after machine creation
        logger.info().measureTime({ "preparation: creating JcMachine" }) {
            JcMachine(
                cp = jcDbContainer.cp,
                // TODO usvm-sbft: we may want to tune UMachineOptions for contest
                options = UMachineOptions(timeoutMs = symbolicBudgetPerMethod)
            )
        }.use { machine ->
            statsForClass.methodsCount = filteredMethods.size

            // nothing to process further
            if (filteredMethods.isEmpty()) return@runBlocking statsForClass

            for (method in filteredMethods) {
                val jcClass = jcDbContainer.cp.findClass(method.classId.name)

                val jcTypedMethod = jcClass.toType().declaredMethods.firstOrNull {
                    it.name == method.name && it.method.jcdbSignature == when (method) {
                        is ConstructorId -> method.constructor.jcdbSignature
                        is MethodId -> method.method.jcdbSignature
                    }
                }
                if (jcTypedMethod == null) {
                    logger.error { "Method [$method] not found in jcClass [$jcClass]" }
                    continue
                }
                val states = logger.debug().measureTime({ "machine.analyze(${method.classId}.${method.signature})" }) {
                    ((ThreadBasedExecutor.threadLocal.invokeWithTimeout(10 * symbolicBudgetPerMethod) {
                        machine.analyze(jcTypedMethod.method)
                    } as? Result<List<JcState>>) ?: run {
                        logger.error { "machine.analyze(${jcTypedMethod.method}) timed out" }
                        Result.success(emptyList())
                    }).getOrElse { e ->
                        logger.error("JcMachine failed", e)
                        emptyList()
                    }
                }
                val jcExecutions = states.mapNotNull {
                    if (remainingBudgetMillisWithoutCodegen() > UTestRunner.CONTEST_TEST_EXECUTION_TIMEOUT.inWholeMilliseconds)
                        logger.debug().measureTime({ "resolver.resolve(${method.classId}.${method.signature}, ...)" }) {
                            runCatching {
                                resolver.resolve(jcTypedMethod, it)
                            }.getOrElse { e ->
                                logger.error(e) { "Resolver failed" }
                                null
                            }
                        }
                    else null
                }

                var testsCounter = 0
                val statsForMethod = StatsForMethod(
                    "${method.classId.simpleName}#${method.name}",
                    expectedExceptions.getForMethod(method.name).exceptionNames
                )
                statsForClass.statsForMethods.add(statsForMethod)

                val utExecutions: List<UtExecution> = jcExecutions.mapNotNull {
                    logger.debug().measureTime({ "Convert JcExecution" }) {
                        try {
                            JcToUtExecutionConverter(
                                jcExecution = it,
                                jcClasspath = jcDbContainer.cp,
                                idGenerator = idGenerator,
                                instructionIdProvider = instructionIdProvider,
                                utilMethodProvider = codeGenerator.context.utilMethodProvider
                            ).convert()
                        } catch (e: Exception) {
                            logger.error(e) {
                                "Can't convert execution for method ${method.name}, exception is  ${e.message}"
                            }
                            null
                        }
                    }
                }

                utExecutions.forEach { result ->
                    try {
                        val testMethodName = testMethodName(method.toString(), ++testsCounter)
                        val className = Type.getInternalName(method.classId.jClass)
                        logger.debug { "--new testCase collected, to generate: $testMethodName" }
                        statsForMethod.testsGeneratedCount++
                        result.result.exceptionOrNull()?.let { exception ->
                            statsForMethod.detectedExceptionFqns += exception::class.java.name
                        }
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
                        logger.error(e) { "Test generation failed during stats update" }
                    }
                }
                logger.debug { "Finished $method" }
            }
        }
    }

    val testSets = testsByMethod.map { (method, executions) ->
        UtMethodTestSet(method, minimizeExecutions(executions), jimpleBody = null)
    }.summarizeAll()

    logger.info().measureTime({ "Flushing tests for [${cut.simpleName}] on disk" }) {
        writeTestClass(cut, codeGenerator.generateAsString(testSets))
    }

    logger.debug { "STOPPED COUNTING BUDGET FOR ${cut.classId.name}" }

    statsForClass
}