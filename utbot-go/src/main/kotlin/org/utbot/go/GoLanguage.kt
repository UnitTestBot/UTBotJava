package org.utbot.go

import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.IdentityTrie
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
    GoPointerValueProvider,
    GoInterfaceValueProvider
)

data class CoveredLines(val lines: Set<String>)

class GoDescription(
    val worker: GoWorker,
    val functionUnderTest: GoUtFunction,
    val coverage: Trie<String, String>,
    val intSize: Int
) : Description<GoTypeId>(
    if (functionUnderTest.isMethod) {
        listOf(functionUnderTest.receiver!!.type) + functionUnderTest.parameters.map { it.type }.toList()
    } else {
        functionUnderTest.parameters.map { it.type }.toList()
    }
)

suspend fun runGoFuzzing(
    function: GoUtFunction,
    worker: GoWorker,
    index: Int,
    intSize: Int,
    providers: List<ValueProvider<GoTypeId, GoUtModel, GoDescription>> = goDefaultValueProviders(),
    exec: suspend (description: GoDescription, values: List<GoUtModel>) -> BaseFeedback<Trie.Node<String>, GoTypeId, GoUtModel>
) {
    BaseFuzzing(providers, exec).fuzz(
        description = GoDescription(
            worker = worker,
            functionUnderTest = function,
            coverage = IdentityTrie(),
            intSize = intSize
        ),
        random = Random(index)
    )
}