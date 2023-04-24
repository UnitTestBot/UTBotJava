package org.utbot.go

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.fuzzing.BaseFeedback
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.utils.Trie
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.worker.GoWorker
import org.utbot.go.worker.RawExecutionResult

val logger = KotlinLogging.logger {}

class GoEngine(
    private val worker: GoWorker,
    private val functionUnderTest: GoUtFunction,
    private val aliases: Map<GoPackage, String?>,
    private val intSize: Int,
    private val timeoutExceededOrIsCanceled: () -> Boolean,
) {
    var numberOfFunctionExecutions: Int = 0

    fun fuzzing(): Flow<Pair<GoUtFuzzedFunction, RawExecutionResult>> = flow {
        var attempts = 0
        val attemptsLimit = Int.MAX_VALUE
        if (functionUnderTest.parameters.isEmpty()) {
            worker.sendFuzzedParametersValues(functionUnderTest, emptyList(), emptyMap())
            val rawExecutionResult = worker.receiveRawExecutionResult()
            val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, emptyList())
            emit(fuzzedFunction to rawExecutionResult)
        } else {
            val notCoveredLines = (1..functionUnderTest.numberOfAllStatements).toMutableSet()
            runGoFuzzing(functionUnderTest, intSize) { description, values ->
                if (timeoutExceededOrIsCanceled() || notCoveredLines.isEmpty()) {
                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                }
                val fuzzedFunction = GoUtFuzzedFunction(functionUnderTest, values)
                worker.sendFuzzedParametersValues(functionUnderTest, values, aliases)
                val rawExecutionResult = worker.receiveRawExecutionResult()
                numberOfFunctionExecutions++
                if (rawExecutionResult.trace.isEmpty()) {
                    logger.error { "Coverage is empty for [${functionUnderTest.name}] with $values}" }
                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                }
                val trieNode = description.tracer.add(rawExecutionResult.trace.map { GoInstruction(it) })
                if (trieNode.count > 1) {
                    if (++attempts >= attemptsLimit) {
                        return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                    }
                    return@runGoFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
                }
                if (notCoveredLines.removeAll(rawExecutionResult.trace.toSet())) {
                    emit(fuzzedFunction to rawExecutionResult)
                }
                BaseFeedback(result = trieNode, control = Control.CONTINUE)
            }
        }
    }
}