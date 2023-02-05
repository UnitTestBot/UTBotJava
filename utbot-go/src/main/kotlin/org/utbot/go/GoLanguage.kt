package org.utbot.go

import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.Trie
import org.utbot.go.api.GoUtFunction
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.fuzzer.providers.GoArrayValueProvider
import org.utbot.go.fuzzer.providers.GoPrimitivesValueProvider
import org.utbot.go.fuzzer.providers.GoStructValueProvider


fun goDefaultValueProviders() = listOf(
    GoPrimitivesValueProvider, GoArrayValueProvider, GoStructValueProvider
)

class GoInstruction(
    val lineNumber: Int
)

class GoDescription(
    val methodUnderTest: GoUtFunction,
    val tracer: Trie<GoInstruction, *>
) : Description<GoTypeId>(methodUnderTest.parameters.map { it.type }.toList())

suspend fun runGoFuzzing(
    methodUnderTest: GoUtFunction,
    providers: List<ValueProvider<GoTypeId, GoUtModel, GoDescription>> = goDefaultValueProviders(),
    exec: suspend (description: GoDescription, values: List<GoUtModel>) -> BaseFeedback<Trie.Node<GoInstruction>, GoTypeId, GoUtModel>
) {
    BaseFuzzing(providers, exec).fuzz(GoDescription(methodUnderTest, Trie(GoInstruction::lineNumber)))
}