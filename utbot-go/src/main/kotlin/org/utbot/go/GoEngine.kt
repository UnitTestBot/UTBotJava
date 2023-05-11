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
import java.util.concurrent.atomic.AtomicInteger

val logger = KotlinLogging.logger {}

class GoEngine(
    private var workers: List<GoWorker>,
    private val functionUnderTest: GoUtFunction,
    private val needToCoverLines: List<String>,
    private val aliases: Map<GoPackage, String?>,
    private val intSize: Int,
    private val functionExecutionTimeoutMillis: Long,
    private val fuzzingMode: Boolean,
    private val timeoutExceededOrIsCanceled: () -> Boolean
) {
    var numberOfFunctionExecutions: AtomicInteger = AtomicInteger(0)

    fun fuzzing(): Flow<GoUtFuzzedFunctionTestCase> = channelFlow {
        if (!functionUnderTest.isMethod && functionUnderTest.parameters.isEmpty()) {
            workers[0].sendFuzzedParametersValues(functionUnderTest, emptyList(), emptyMap())
            val executionResult = run {
                val rawExecutionResult = workers[0].receiveRawExecutionResult()
                convertRawExecutionResultToExecutionResult(
                    rawExecutionResult = rawExecutionResult,
                    functionResultTypes = functionUnderTest.results.map { it.type },
                    intSize = intSize,
                    timeoutMillis = functionExecutionTimeoutMillis,
                )
            }
            val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, emptyList())
            send(GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult))
        } else {
            val mutex = Mutex(false)
            val notCoveredLines = needToCoverLines.toMutableSet()
            workers.mapIndexed { index, worker ->
                launch(Dispatchers.IO) {
                    var attempts = 0
                    val attemptsLimit = Int.MAX_VALUE
                    runGoFuzzing(functionUnderTest, worker, index, intSize) { description, values ->
                        try {
                            if (timeoutExceededOrIsCanceled() || notCoveredLines.isEmpty()) {
                                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                            }
                            val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, values)
                            val (executionResult, coverTab) = run {
                                description.worker.sendFuzzedParametersValues(functionUnderTest, values, aliases)
                                val rawExecutionResult = description.worker.receiveRawExecutionResult()
                                println(rawExecutionResult)
                                convertRawExecutionResultToExecutionResult(
                                    rawExecutionResult = rawExecutionResult,
                                    functionResultTypes = functionUnderTest.results.map { it.type },
                                    intSize = intSize,
                                    timeoutMillis = functionExecutionTimeoutMillis,
                                ) to rawExecutionResult.coverTab
                            }
                            numberOfFunctionExecutions.incrementAndGet()
                            if (coverTab.isEmpty()) {
                                logger.error { "Coverage is empty for [${functionUnderTest.name}] with $values}" }
                                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                            }
                            val trieNode = description.coverage.add(coverTab.keys.sorted())
                            if (trieNode.count > 1) {
                                if (++attempts >= attemptsLimit) {
                                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                                }
                                return@runGoFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
                            }
                            mutex.withLock {
                                if (fuzzingMode) {
                                    if (executionResult is GoUtExecutionWithNonNilError || executionResult is GoUtPanicFailure) {
                                        if (notCoveredLines.isNotEmpty()) {
                                            notCoveredLines.clear()
                                            send(GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult))
                                        }
                                        return@runGoFuzzing BaseFeedback(
                                            result = Trie.emptyNode(),
                                            control = Control.STOP
                                        )
                                    }
                                } else if (notCoveredLines.removeAll(coverTab.keys)) {
                                    send(GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult))
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