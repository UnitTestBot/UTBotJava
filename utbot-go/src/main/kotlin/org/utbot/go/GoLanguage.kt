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
    GoPointerValueProvider
)

class GoDescription(
    val worker: GoWorker,
    val methodUnderTest: GoUtFunction,
    val coverage: Trie<String, String>,
    val intSize: Int
) : Description<GoTypeId>(methodUnderTest.parameters.map { it.type }.toList())

suspend fun runGoFuzzing(
    methodUnderTest: GoUtFunction,
    worker: GoWorker,
    index: Int,
    intSize: Int,
    providers: List<ValueProvider<GoTypeId, GoUtModel, GoDescription>> = goDefaultValueProviders(),
    exec: suspend (description: GoDescription, values: List<GoUtModel>) -> BaseFeedback<Trie.Node<String>, GoTypeId, GoUtModel>
) {
    BaseFuzzing(providers, exec).fuzz(
        description = GoDescription(
            worker = worker,
            methodUnderTest = methodUnderTest,
            coverage = IdentityTrie(),
            intSize = intSize
        ),
        random = Random(index)
    )
}