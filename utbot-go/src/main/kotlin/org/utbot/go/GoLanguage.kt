package org.utbot.go

import kotlinx.coroutines.sync.Mutex
import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.Trie
import org.utbot.go.api.GoUtFunction
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.fuzzer.providers.*
import org.utbot.go.worker.GoWorker
import kotlin.random.Random


fun goDefaultValueProviders() = listOf(
    GoPrimitivesValueProvider,
    GoArrayValueProvider,
    GoSliceValueProvider,
    GoMapValueProvider,
    GoChanValueProvider,
    GoStructValueProvider,
    GoConstantValueProvider,
    GoNamedValueProvider,
    GoNilValueProvider,
    GoPointerValueProvider
)

class GoInstruction(
    val lineNumber: Int
)

class GoDescription(
    val mutex: Mutex,
    val worker: GoWorker,
    val methodUnderTest: GoUtFunction,
    val tracer: Trie<GoInstruction, *>,
    val intSize: Int
) : Description<GoTypeId>(methodUnderTest.parameters.map { it.type }.toList())

suspend fun runGoFuzzing(
    mutex: Mutex,
    methodUnderTest: GoUtFunction,
    worker: GoWorker,
    index: Int,
    intSize: Int,
    providers: List<ValueProvider<GoTypeId, GoUtModel, GoDescription>> = goDefaultValueProviders(),
    exec: suspend (description: GoDescription, values: List<GoUtModel>) -> BaseFeedback<Trie.Node<GoInstruction>, GoTypeId, GoUtModel>
) {
    BaseFuzzing(providers, exec).fuzz(
        description = GoDescription(
            mutex = mutex,
            worker = worker,
            methodUnderTest = methodUnderTest,
            tracer = Trie(GoInstruction::lineNumber),
            intSize = intSize
        ),
        random = Random(index)
    )
}