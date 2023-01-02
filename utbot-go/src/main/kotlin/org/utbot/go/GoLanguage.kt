package org.utbot.go

import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.Trie
import org.utbot.go.api.GoTypeId
import org.utbot.go.api.GoUtFunction
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
    description: FuzzedMethodDescription,
    val tracer: Trie<GoInstruction, *>
) : Description<GoTypeId>(description.parameters.map { it as GoTypeId }.toList())

suspend fun runGoFuzzing(
    methodUnderTest: GoUtFunction,
    providers: List<ValueProvider<GoTypeId, FuzzedValue, GoDescription>> = goDefaultValueProviders(),
    exec: suspend (description: GoDescription, values: List<FuzzedValue>) -> BaseFeedback<Trie.Node<GoInstruction>, GoTypeId, FuzzedValue>
) {
    val fmd = FuzzedMethodDescription(
        methodUnderTest.name,
        methodUnderTest.resultTypesAsGoClassId,
        methodUnderTest.parametersTypes,
        methodUnderTest.concreteValues
    )
    BaseFuzzing(providers, exec).fuzz(
        GoDescription(
            fmd, Trie(GoInstruction::lineNumber)
        )
    )
}