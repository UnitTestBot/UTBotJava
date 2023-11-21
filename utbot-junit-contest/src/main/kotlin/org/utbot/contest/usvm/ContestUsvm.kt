package org.utbot.contest.usvm

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcMethod
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.ext.findClass
import org.jacodb.api.ext.jcdbSignature
import org.jacodb.api.ext.toType
import org.jacodb.approximation.Approximations
import org.jacodb.impl.features.InMemoryHierarchy
import org.objectweb.asm.Type
import org.usvm.UMachineOptions
import org.usvm.instrumentation.util.jcdbSignature
import org.usvm.machine.state.JcState
import org.utbot.common.ThreadBasedExecutor
import org.utbot.common.info
import org.utbot.common.measureTime
import org.utbot.contest.*
import org.utbot.contest.junitVersion
import org.utbot.contest.usvm.converter.JcToUtExecutionConverter
import org.utbot.contest.usvm.converter.SimpleInstructionIdProvider
import org.utbot.contest.usvm.converter.toExecutableId
import org.utbot.contest.usvm.jc.JcContainer
import org.utbot.contest.usvm.jc.JcContainer.Companion.CONTEST_TEST_EXECUTION_TIMEOUT
import org.utbot.contest.usvm.jc.JcTestExecutor
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
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

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

    // 15% to terminate all activities and finalize code in file
    val generationTimeoutMillisWithoutCodegen: Long = timeBudgetMs - timeBudgetMs * 15 / 100
    // 15% for instrumentation
    // TODO usvm-sbft: when `jcMachine.analyzeAsync(): Flow<JcState>` is added run `analyze` and `execute` under common timeout
    val jcMachineTimeoutMillis: Long = generationTimeoutMillisWithoutCodegen - timeBudgetMs * 15 / 100

    logger.debug { "-----------------------------------------------------------------------------" }
    logger.info(
        "Contest.runGeneration: Time budget: $timeBudgetMs ms, jcMachine timeout=$jcMachineTimeoutMillis ms, " +
                "classpath=$classpathString, methodNameFilter=$methodNameFilter"
    )

    val classpathFiles = classpathString.split(File.pathSeparator).map { File(it) }

    val jcContainer by lazy {
        JcContainer(
            classpath = classpathFiles,
            machineOptions = UMachineOptions(timeout = jcMachineTimeoutMillis.milliseconds)
        ) {
            // TODO usvm-sbft: we may want to tune these JcSettings for contest
            useJavaRuntime(JdkInfoService.provide().path.toFile())
            installFeatures(InMemoryHierarchy, Approximations)
            loadByteCode(classpathFiles)
        }
    }

    val executor by lazy { JcTestExecutor(jcContainer.cp, jcContainer.runner) }

    val utModelIdGenerator = ReferencePreservingIntIdGenerator()
    val instructionIdGenerator = SimpleInstructionIdProvider()

    if (runFromEstimator) {
        setOptions() // UtBot options (aka UtSettings)
        // will not be executed in real contest (see ContestKt.main() for more details)
        logger.info().measureTime({ "warmup: 1st optional JacoDB initialization (not to be counted in time budget)" }) {
            jcContainer // force init lazy property
        }
        logger.info().measureTime({ "warmup: 1st optional executor start (not to be counted in time budget)" }) {
            jcContainer.runner.ensureRunnerAlive()
        }
    }

    logger.info { "STARTED COUNTING BUDGET FOR ${cut.classId.name}" }

    val startTime = System.currentTimeMillis()
    fun remainingBudgetMillisWithoutCodegen() =
        max(0, generationTimeoutMillisWithoutCodegen - (System.currentTimeMillis() - startTime))

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
            jcContainer // force init lazy property
        }
        logger.info().measureTime({ "preparation: ensure executor is started (counted in time budget)" }) {
            jcContainer.runner.ensureRunnerAlive()
        }

        statsForClass.methodsCount = filteredMethods.size
        val methodToStats = filteredMethods.associateWith { method ->
            StatsForMethod(
                "${method.classId.simpleName}#${method.name}",
                expectedExceptions.getForMethod(method.name).exceptionNames
            )
        }.onEach { (_, statsForMethod) -> statsForClass.statsForMethods.add(statsForMethod) }

        val jcMethods = filteredMethods.mapNotNull { methodId ->
            jcContainer.cp.findMethodOrNull(methodId).also {
                if (it == null) logger.error { "Method [$methodId] not found in jcClasspath [${jcContainer.cp}]" }
            }
        }

        // nothing to process further
        if (jcMethods.isEmpty()) return@runBlocking statsForClass

        val states = logger.info().measureTime({ "machine.analyze(${cut.classId.name})" }) {
            ((ThreadBasedExecutor.threadLocal.invokeWithTimeout(jcMachineTimeoutMillis * 11 / 10) {
                // TODO usvm-sbft: sometimes `machine.analyze` hangs forever, completely ignoring timeout specified for it
                jcContainer.machine.analyze(jcMethods, targets = emptyList())
            } as? Result<List<JcState>>) ?: run {
                logger.error { "machine.analyze(${cut.classId.name}) timed out" }
                Result.success(emptyList())
            }).getOrElse { e ->
                logger.error(e) { "machine.analyze(${cut.classId.name}) failed" }
                emptyList()
            }
        }

        val jcExecutions = logger.info().measureTime({ "executor.execute(${cut.classId.name})" }) {
            states.mapNotNull { state ->
                // TODO usvm-sbft: if we have less than CONTEST_TEST_EXECUTION_TIMEOUT time left, we should
                //  try executing with smaller timeout, but instrumentation currently doesn't allow to change timeout
                if (remainingBudgetMillisWithoutCodegen() > CONTEST_TEST_EXECUTION_TIMEOUT.inWholeMilliseconds) {
                    runCatching {
                        executor.execute(state.entrypoint.typedMethod, state)
                    }.getOrElse { e ->
                        logger.error(e) { "executor.execute(${state.entrypoint}) failed" }
                        null
                    }
                } else {
                    logger.warn { "executor.execute(${cut.classId.name}) run out of time" }
                    null
                }
            }
        }

        val utExecutions = logger.info().measureTime({"JcToUtExecutionConverter.convert(${cut.classId.name})"}) {
            jcExecutions.mapNotNull { jcExecution ->
                try {
                    val methodId = jcExecution.method.method.toExecutableId(jcContainer.cp)
                    JcToUtExecutionConverter(
                        jcExecution = jcExecution,
                        jcClasspath = jcContainer.cp,
                        idGenerator = utModelIdGenerator,
                        instructionIdProvider = instructionIdGenerator,
                        utilMethodProvider = codeGenerator.context.utilMethodProvider
                    ).convert()?.let { it to methodId }
                } catch (e: Exception) {
                    logger.error(e) { "JcToUtExecutionConverter.convert(${jcExecution.method.method}) failed" }
                    null
                }
            }
        }

        logger.info().measureTime({"Collect stats for ${cut.classId.name}"}) {
            utExecutions.forEach { (utExecution, methodId) ->
                try {
                    val className = Type.getInternalName(methodId.classId.jClass)
                    val statsForMethod = methodToStats.getValue(methodId)
                    statsForMethod.testsGeneratedCount++
                    utExecution.result.exceptionOrNull()?.let { exception ->
                        statsForMethod.detectedExceptionFqns += exception::class.java.name
                    }
                    utExecution.coverage?.let {
                        statsForClass.updateCoverage(
                            newCoverage = it,
                            isNewClass = !statsForClass.testedClassNames.contains(className),
                            fromFuzzing = utExecution is UtFuzzedExecution
                        )
                    }
                    statsForClass.testedClassNames.add(className)
                    testsByMethod.getOrPut(methodId) { mutableListOf() } += utExecution
                } catch (e: Throwable) {
                    logger.error(e) { "Test generation failed during stats update for $methodId" }
                }
            }
        }
    }

    val testSets = logger.info().measureTime({ "Code generation for ${cut.classId.name}" }) {
        testsByMethod.map { (method, executions) ->
            UtMethodTestSet(method, minimizeExecutions(executions), jimpleBody = null)
        }.summarizeAll()
    }

    logger.info().measureTime({ "Flushing tests for [${cut.classId.name}] on disk" }) {
        writeTestClass(cut, codeGenerator.generateAsString(testSets))
    }

    logger.info { "STOPPED COUNTING BUDGET FOR ${cut.classId.name}" }

    statsForClass
}

fun JcClasspath.findMethodOrNull(method: ExecutableId): JcMethod? =
    findClass(method.classId.name).declaredMethods.firstOrNull {
        it.name == method.name && it.jcdbSignature == method.jcdbSignature
    }

val JcMethod.typedMethod: JcTypedMethod get() = enclosingClass.toType().declaredMethods.first {
    it.name == name && it.method.jcdbSignature == jcdbSignature
}

val ExecutableId.jcdbSignature: String get() = when (this) {
    is ConstructorId -> constructor.jcdbSignature
    is MethodId -> method.jcdbSignature
}
