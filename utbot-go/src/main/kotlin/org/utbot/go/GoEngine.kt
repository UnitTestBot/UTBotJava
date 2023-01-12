package org.utbot.go

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.utbot.fuzzing.BaseFeedback
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.utils.Trie
import org.utbot.go.api.GoUtExecutionResult
import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.executor.GoFuzzedFunctionsExecutor
import org.utbot.go.logic.EachExecutionTimeoutsMillisConfig

class GoEngine(
    private val methodUnderTest: GoUtFunction,
    private val sourceFile: GoUtFile,
    private val goExecutableAbsolutePath: String,
    private val eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig
) {
    fun fuzzing(): Flow<Pair<GoUtFuzzedFunction, GoUtExecutionResult>> = flow {
        var attempts = 0
        val attemptsLimit = 100
        val linesToCover = (1..methodUnderTest.numberOfAllStatements).toMutableSet()
        runGoFuzzing(methodUnderTest) { description, values ->
            val fuzzedFunction = GoUtFuzzedFunction(methodUnderTest, values)
            val executionResult = GoFuzzedFunctionsExecutor.executeGoSourceFileFuzzedFunctions(
                sourceFile,
                fuzzedFunction,
                goExecutableAbsolutePath,
                eachExecutionTimeoutsMillisConfig
            )
            linesToCover.removeAll(executionResult.trace.toSet())
            val trieNode = description.tracer.add(executionResult.trace.map { GoInstruction(it) })
            if (trieNode.count > 1) {
                if (++attempts >= attemptsLimit) {
                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                }
                return@runGoFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
            }
            emit(fuzzedFunction to executionResult)
            if (linesToCover.isEmpty()) {
                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
            }
            BaseFeedback(result = trieNode, control = Control.CONTINUE)
        }
    }
}