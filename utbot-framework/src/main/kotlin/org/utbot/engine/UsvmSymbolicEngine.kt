package org.utbot.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
import org.utbot.usvm.converter.JcToUtExecutionConverter
import org.utbot.usvm.converter.SimpleInstructionIdProvider
import org.utbot.usvm.converter.UtExecutionInitialState
import org.utbot.usvm.converter.toExecutableId
import org.utbot.usvm.jc.JcContainer
import org.utbot.usvm.jc.JcExecution
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
    ): Flow<Pair<ExecutableId, UtResult>> = flow {
        var analysisResult: AnalysisResult? = null

        val classpathFiles = classpath.split(File.pathSeparator).map { File(it) }
        val jcContainer = createJcContainer(classpathFiles)

        val jcMethods = methods
            .mapNotNull { methodId -> jcContainer.cp.findMethodOrNull(methodId).also {
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
                val jcExecutionConstruction = constructJcExecution(jcMachine, state, jcContainer)

                val jcExecution = jcExecutionConstruction.jcExecution
                val executableId = jcExecution.method.method.toExecutableId(jcContainer.cp)

                val executionConverter = JcToUtExecutionConverter(
                    jcExecution = jcExecution,
                    jcClasspath = jcContainer.cp,
                    idGenerator = ReferencePreservingIntIdGenerator(),
                    instructionIdProvider = SimpleInstructionIdProvider(),
                    utilMethodProvider = UtilMethodProviderPlaceholder,
                )

                val utResult = if (jcExecutionConstruction.useUsvmExecutor) {
                    executionConverter.convert()
                } else {
                    val initialState = executionConverter.convertInitialStateOnly()
                    val concreteExecutor = ConcreteExecutor(concreteExecutionContext.instrumentationFactory, classpath)
                        .apply { this.classLoader = utContext.classLoader }

                    runStandardConcreteExecution(concreteExecutor, executableId, initialState)
                }

                utResult?.let {
                    analysisResult = AnalysisResult(executableId, it)
                }
            }
        }

        analysisResult?.let {
            emit(it.executableId to it.utResult)
        }
    }

    private fun constructJcExecution(
        jcMachine: JcMachine,
        state: JcState,
        jcContainer: JcContainer,
    ): JcExecutionConstruction {
        val executor = JcTestExecutor(jcContainer.cp, jcContainer.runner)

        val realJcExecution = runCatching {
            executor.execute(
                method = state.entrypoint.typedMethod,
                state = state,
                stringConstants = jcMachine.stringConstants,
                classConstants = jcMachine.classConstants,
                allowSymbolicResult = false
            )
        }.getOrElse { null }

        realJcExecution?.let {
            return JcExecutionConstruction(
                jcExecution = it,
                useUsvmExecutor = true,
            )
        }

        val jcExecutionWithUTest = JcExecution(
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

        return JcExecutionConstruction(
            jcExecution = jcExecutionWithUTest,
            useUsvmExecutor = false,
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

            UtSymbolicExecution(
                initialState.stateBefore,
                concreteExecutionResult.stateAfter,
                concreteExecutionResult.result,
                concreteExecutionResult.newInstrumentation ?: initialState.instrumentations,
                mutableListOf(),
                listOf(),
                concreteExecutionResult.coverage
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
        installFeatures(InMemoryHierarchy, Approximations, ClassScorer(TypeScorer, ::scoreClassNode))
        loadByteCode(classpathFiles)
    }

    data class JcExecutionConstruction(
        val jcExecution: JcExecution,
        val useUsvmExecutor: Boolean,
    )

    data class AnalysisResult(
        val executableId: ExecutableId,
        val utResult: UtResult,
    )
}