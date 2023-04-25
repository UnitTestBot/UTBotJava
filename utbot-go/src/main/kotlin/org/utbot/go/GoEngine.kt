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
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.worker.GoWorker
import org.utbot.go.worker.RawExecutionResult
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

val logger = KotlinLogging.logger {}

class GoEngine(
    private var workers: List<GoWorker>,
    private val functionUnderTest: GoUtFunction,
    private val aliases: Map<GoPackage, String?>,
    private val intSize: Int,
    private val timeoutExceededOrIsCanceled: () -> Boolean
) {
    var numberOfFunctionExecutions: AtomicInteger = AtomicInteger(0)

    fun fuzzing(): Flow<Pair<GoUtFuzzedFunction, RawExecutionResult>> = channelFlow {
        var attempts = 0
        val attemptsLimit = Int.MAX_VALUE
        if (functionUnderTest.parameters.isEmpty()) {
            workers[0].sendFuzzedParametersValues(functionUnderTest, emptyList(), emptyMap())
            val rawExecutionResult = workers[0].receiveRawExecutionResult()
            val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, emptyList())
            send(fuzzedFunction to rawExecutionResult)
        } else {
            val mutex = Mutex(false)
            val notCoveredLines = (1..functionUnderTest.numberOfAllStatements).toMutableSet()
            workers.mapIndexed { index, worker ->
                launch(Dispatchers.IO) {
                    runGoFuzzing(mutex, functionUnderTest, worker, index, intSize) { description, values ->
                        try {
                            if (timeoutExceededOrIsCanceled() || notCoveredLines.isEmpty()) {
                                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                            }
                            val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, values)
                            val rawExecutionResult = run {
                                description.worker.sendFuzzedParametersValues(functionUnderTest, values, aliases)
                                description.worker.receiveRawExecutionResult()
                            }
                            numberOfFunctionExecutions.incrementAndGet()
                            if (rawExecutionResult.trace.isEmpty()) {
                                logger.error { "Coverage is empty for [${functionUnderTest.name}] with $values}" }
                                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                            }
                            val trieNode = description.mutex.withLock {
                                description.tracer.add(rawExecutionResult.trace.map { GoInstruction(it) })
                            }
                            if (trieNode.count > 1) {
                                if (++attempts >= attemptsLimit) {
                                    return@runGoFuzzing BaseFeedback(
                                        result = Trie.emptyNode(),
                                        control = Control.STOP
                                    )
                                }
                                return@runGoFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
                            }
                            description.mutex.withLock {
                                if (notCoveredLines.removeAll(rawExecutionResult.trace.toSet())) {
                                    send(fuzzedFunction to rawExecutionResult)
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