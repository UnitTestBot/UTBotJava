package org.utbot.engine

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jacodb.approximation.Approximations
import org.jacodb.impl.features.InMemoryHierarchy
import org.usvm.PathSelectionStrategy
import org.usvm.PathSelectorFairnessStrategy
import org.usvm.SolverType
import org.usvm.UMachineOptions
import org.usvm.api.JcCoverage
import org.usvm.machine.JcMachine
import org.usvm.machine.state.JcState
import org.usvm.types.ClassScorer
import org.usvm.types.TypeScorer
import org.usvm.types.scoreClassNode
import org.usvm.util.ApproximationPaths
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.builtin.UtilMethodProviderPlaceholder
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.fuzzer.ReferencePreservingIntIdGenerator
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.InstrumentedProcessDeathException
import org.utbot.framework.plugin.api.UtConcreteExecutionFailure
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtFailedExecution
import org.utbot.framework.plugin.api.UtResult
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
import org.utbot.usvm.converter.JcToUtExecutionConverter
import org.utbot.usvm.converter.SimpleInstructionIdProvider
import org.utbot.usvm.converter.UtExecutionInitialState
import org.utbot.usvm.converter.UtUsvmExecution
import org.utbot.usvm.converter.toExecutableId
import org.utbot.usvm.jc.JcContainer
import org.utbot.usvm.jc.JcExecution
import org.utbot.usvm.jc.JcJars
import org.utbot.usvm.jc.JcTestExecutor
import org.utbot.usvm.jc.findMethodOrNull
import org.utbot.usvm.jc.typedMethod
import org.utbot.usvm.machine.analyzeAsync
import java.io.File
import java.util.concurrent.CancellationException
import kotlin.time.Duration.Companion.milliseconds

object UsvmSymbolicEngine {

    private val logger = KotlinLogging.logger {}

    fun runUsvmGeneration(
        methods: List<ExecutableId>,
        classpath: String,
        concreteExecutionContext: ConcreteExecutionContext,
        timeoutMillis: Long
    ): List<Pair<ExecutableId, UtResult>> {

        val collectedExecutions = mutableListOf<Pair<ExecutableId, UtResult>>()
        val classpathFiles = classpath.split(File.pathSeparator).map { File(it) }

        createJcContainer(classpathFiles).use { jcContainer ->
            val jcMethods = methods
                .mapNotNull { methodId ->
                    jcContainer.cp.findMethodOrNull(methodId).also {
                        if (it == null) {
                            logger.error { "Method [$methodId] not found in jcClasspath [${jcContainer.cp}]" }
                        }
                    }
                }

            JcMachine(
                cp = jcContainer.cp,
                options = UMachineOptions(
                    timeout = timeoutMillis.milliseconds,
                    pathSelectionStrategies = listOf(PathSelectionStrategy.CLOSEST_TO_UNCOVERED_RANDOM),
                    pathSelectorFairnessStrategy = PathSelectorFairnessStrategy.COMPLETELY_FAIR,
                    solverType = SolverType.Z3,
                )
            ).use { jcMachine ->
                jcMachine.analyzeAsync(
                    forceTerminationTimeout = (timeoutMillis * 1.1 + 2000).toLong(),
                    methods = jcMethods,
                    targets = emptyList()
                ) { state ->
                    val jcExecution = constructJcExecution(jcMachine, state, jcContainer)

                    val executableId = jcExecution.method.method.toExecutableId(jcContainer.cp)

                    val executionConverter = JcToUtExecutionConverter(
                        jcExecution = jcExecution,
                        jcClasspath = jcContainer.cp,
                        idGenerator = ReferencePreservingIntIdGenerator(),
                        instructionIdProvider = SimpleInstructionIdProvider(),
                        utilMethodProvider = UtilMethodProviderPlaceholder,
                    )

                    val utResult = runCatching {
                        executionConverter.convert()
                    }.getOrElse { e ->
                        logger.warn(e) { "JcToUtExecutionConverter.convert(${jcExecution.method.method}) failed" }
                        val initialState = executionConverter.convertInitialStateOnly()
                        val concreteExecutor =
                            ConcreteExecutor(concreteExecutionContext.instrumentationFactory, classpath)
                                .apply { this.classLoader = utContext.classLoader }

                        runStandardConcreteExecution(concreteExecutor, executableId, initialState)
                    }

                    utResult?.let {
                        collectedExecutions.add(executableId to it)
                    }
                }
            }
        }

        return collectedExecutions
    }

    private fun constructJcExecution(
        jcMachine: JcMachine,
        state: JcState,
        jcContainer: JcContainer,
    ): JcExecution {
        val executor = JcTestExecutor(jcContainer.cp, jcContainer.runner)

        val realJcExecution = runCatching {
            executor.execute(
                method = state.entrypoint.typedMethod,
                state = state,
                stringConstants = jcMachine.stringConstants,
                classConstants = jcMachine.classConstants,
                allowSymbolicResult = false
            )
        }.getOrElse { e ->
            logger.warn(e) { "executor.execute(${state.entrypoint}) failed" }
            null
        }

        realJcExecution?.let {
            return it
        }

        return JcExecution(
            method = state.entrypoint.typedMethod,
            uTest = executor.createUTest(
                method = state.entrypoint.typedMethod,
                state = state,
                stringConstants = jcMachine.stringConstants,
                classConstants = jcMachine.classConstants,
            ),
            uTestExecutionResultWrappers = emptySequence(),
            coverage = JcCoverage(emptyMap()),
        )
    }

    private fun runStandardConcreteExecution(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
        executableId: ExecutableId,
        initialState: UtExecutionInitialState,
    ): UtResult? {
        return try {
            val concreteExecutionResult = runBlocking {
                concreteExecutor.executeConcretely(
                    executableId,
                    initialState.stateBefore,
                    initialState.instrumentations,
                    UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis,
                )
            }

            concreteExecutionResult.processedFailure()?.let { failure ->
                logger.warn { "Instrumented process failed with exception ${failure.exception} " +
                        "before concrete execution started" }
                null
            } ?: UtUsvmExecution(
                    initialState.stateBefore,
                    concreteExecutionResult.stateAfter,
                    concreteExecutionResult.result,
                    concreteExecutionResult.coverage,
                    instrumentation = concreteExecutionResult.newInstrumentation ?: initialState.instrumentations,
                )
        } catch (e: CancellationException) {
            logger.debug(e) { "Cancellation happened" }
            null
        } catch (e: InstrumentedProcessDeathException) {
            UtFailedExecution(
                stateBefore = initialState.stateBefore,
                result = UtConcreteExecutionFailure(e)
            )
        } catch (e: Throwable) {
            UtError("Concrete execution failed", e)
        }
    }

    private fun createJcContainer(classpathFiles: List<File>) = JcContainer(
        usePersistence = false,
        persistenceDir = utBotTempDirectory.toFile().resolve("jacoDbPersisitenceDirectory"),
        classpath = classpathFiles,
        javaHome = JdkInfoService.provide().path.toFile(),
    ) {
        val approximationPaths = ApproximationPaths(
            usvmApiJarPath = JcJars.approximationsApiJar.absolutePath,
            usvmApproximationsJarPath = JcJars.approximationsJar.absolutePath,
        )
        installFeatures(
            InMemoryHierarchy,
            Approximations,
            ClassScorer(TypeScorer, ::scoreClassNode, approximationPaths)
        )
        loadByteCode(classpathFiles)
    }
}