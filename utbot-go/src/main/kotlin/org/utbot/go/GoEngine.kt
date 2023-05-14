package org.utbot.go

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.utbot.fuzzing.BaseFeedback
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.utils.Trie
import org.utbot.go.api.*
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.worker.GoWorker
import org.utbot.go.worker.convertRawExecutionResultToExecutionResult
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

val logger = KotlinLogging.logger {}

class GoEngine(
    private var workers: List<GoWorker>,
    private val functionUnderTest: GoUtFunction,
    private val needToCoverLines: Set<String>,
    private val aliases: Map<GoPackage, String?>,
    private val intSize: Int,
    private val functionExecutionTimeoutMillis: Long,
    private val fuzzingMode: Boolean,
    private val timeoutExceededOrIsCanceled: () -> Boolean
) {
    var numberOfFunctionExecutions: AtomicInteger = AtomicInteger(0)

    fun fuzzing(): Flow<Map<CoveredLines, ExecutionResults>> = channelFlow {
        if (!functionUnderTest.isMethod && functionUnderTest.parameters.isEmpty()) {
            val lengthOfParameters = workers[0].sendFuzzedParametersValues(functionUnderTest, emptyList(), emptyMap())
            val (executionResult, coverTab) = run {
                val rawExecutionResult = workers[0].receiveRawExecutionResult()
                numberOfFunctionExecutions.incrementAndGet()
                convertRawExecutionResultToExecutionResult(
                    rawExecutionResult = rawExecutionResult,
                    functionResultTypes = functionUnderTest.results.map { it.type },
                    intSize = intSize,
                    timeoutMillis = functionExecutionTimeoutMillis,
                ) to rawExecutionResult.coverTab
            }
            val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, emptyList())
            val testCase = GoUtFuzzedFunctionTestCase(
                fuzzedFunction, executionResult
            )
            send(mapOf(CoveredLines(coverTab.keys) to ExecutionResults(testCase, lengthOfParameters)))
        } else {
            val mutex = Mutex(false)
            val needToStop = AtomicBoolean()
            workers.mapIndexed { index, worker ->
                launch(Dispatchers.IO) {
                    var attempts = 0
                    val attemptsLimit = Int.MAX_VALUE
                    val testCases = mutableMapOf<CoveredLines, ExecutionResults>()
                    runGoFuzzing(functionUnderTest, worker, index, intSize) { description, values ->
                        try {
                            if (needToStop.get() || timeoutExceededOrIsCanceled()) {
                                send(testCases)
                                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                            }

                            val lengthOfParameters =
                                description.worker.sendFuzzedParametersValues(functionUnderTest, values, aliases)
                            val (executionResult, coverTab) = run {
                                val rawExecutionResult = description.worker.receiveRawExecutionResult()
                                numberOfFunctionExecutions.incrementAndGet()
                                convertRawExecutionResultToExecutionResult(
                                    rawExecutionResult = rawExecutionResult,
                                    functionResultTypes = functionUnderTest.results.map { it.type },
                                    intSize = intSize,
                                    timeoutMillis = functionExecutionTimeoutMillis,
                                ) to rawExecutionResult.coverTab
                            }

                            if (coverTab.isEmpty()) {
                                logger.error { "Coverage is empty for [${functionUnderTest.name}] with $values}" }
                                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                            }

                            val coveredLines = CoveredLines(needToCoverLines.intersect(coverTab.keys))
                            val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, values)
                            val testCase = GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult)
                            if (!fuzzingMode) {
                                if (testCases[coveredLines] == null) {
                                    testCases[coveredLines] = ExecutionResults(testCase, lengthOfParameters)
                                } else {
                                    testCases[coveredLines]!!.update(testCase, lengthOfParameters)
                                }
                            }

                            val trieNode = description.coverage.add(coveredLines.lines.sorted())
                            if (trieNode.count > 1) {
                                if (++attempts >= attemptsLimit) {
                                    send(testCases)
                                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                                }
                                return@runGoFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
                            }
                            mutex.withLock {
                                if (fuzzingMode && (executionResult is GoUtExecutionWithNonNilError || executionResult is GoUtPanicFailure)) {
                                    needToStop.set(true)
                                    send(mapOf(coveredLines to ExecutionResults(testCase, lengthOfParameters)))
                                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                                }
                            }
                            BaseFeedback(result = trieNode, control = Control.CONTINUE)
                        } catch (e: SocketTimeoutException) {
                            description.worker.restartWorker()
                            return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                        } catch (e: SocketException) {
                            description.worker.restartWorker()
                            return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                        }
                    }
                }
            }
        }
    }
}