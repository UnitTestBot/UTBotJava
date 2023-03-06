package org.utbot.go

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.fuzzing.BaseFeedback
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.utils.Trie
import org.utbot.go.api.GoUtExecutionResult
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.api.GoUtPanicFailure
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.worker.GoWorker
import org.utbot.go.worker.convertRawExecutionResultToExecutionResult

val logger = KotlinLogging.logger {}

class GoEngine(
    private val worker: GoWorker,
    private val functionUnderTest: GoUtFunction,
    private val aliases: Map<GoPackage, String?>,
    private val intSize: Int,
    private val eachExecutionTimeoutMillis: Long,
    private val timeoutExceededOrIsCanceled: () -> Boolean,
) {

    fun fuzzing(): Flow<Pair<GoUtFuzzedFunction, GoUtExecutionResult>> = flow {
        var attempts = 0
        val attemptsLimit = Int.MAX_VALUE
        if (functionUnderTest.parameters.isEmpty()) {
            worker.sendFuzzedParametersValues(functionUnderTest, emptyList(), emptyMap())
            val rawExecutionResult = worker.receiveRawExecutionResult()
            val executionResult = convertRawExecutionResultToExecutionResult(
                rawExecutionResult,
                functionUnderTest.resultTypes,
                intSize,
                eachExecutionTimeoutMillis,
            )
            val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, emptyList())
            emit(fuzzedFunction to executionResult)
        } else {
            val notCoveredLines = (1..functionUnderTest.numberOfAllStatements).toMutableSet()
            runGoFuzzing(functionUnderTest, intSize) { description, values ->
                if (timeoutExceededOrIsCanceled() || notCoveredLines.isEmpty()) {
                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                }
                val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, values)
                worker.sendFuzzedParametersValues(functionUnderTest, values, aliases)
                val rawExecutionResult = worker.receiveRawExecutionResult()
                val executionResult = convertRawExecutionResultToExecutionResult(
                    rawExecutionResult,
                    functionUnderTest.resultTypes,
                    intSize,
                    eachExecutionTimeoutMillis,
                )
                if (executionResult.trace.isEmpty()) {
                    logger.error { "Coverage is empty for [${functionUnderTest.name}] with $values}" }
                    if (executionResult is GoUtPanicFailure) {
                        logger.error { "Execution completed with panic: ${executionResult.panicValue}" }
                    }
                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                }
                val trieNode = description.tracer.add(executionResult.trace.map { GoInstruction(it) })
                if (trieNode.count > 1) {
                    if (++attempts >= attemptsLimit) {
                        return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                    }
                    return@runGoFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
                }
                if (notCoveredLines.removeAll(executionResult.trace.toSet())) {
                    emit(fuzzedFunction to executionResult)
                }
                BaseFeedback(result = trieNode, control = Control.CONTINUE)
            }
        }
    }
}