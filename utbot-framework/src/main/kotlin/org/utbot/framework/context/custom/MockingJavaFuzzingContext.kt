package org.utbot.framework.context.custom

import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.providers.MapValueProvider
import org.utbot.fuzzing.spring.unit.MockValueProvider
import org.utbot.fuzzing.providers.NullValueProvider
import org.utbot.fuzzing.providers.ObjectValueProvider
import org.utbot.fuzzing.providers.StringValueProvider
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

/**
 * Makes fuzzer mock all types that don't have *specific* [JavaValueProvider],
 * like [MapValueProvider] or [StringValueProvider].
 *
 * NOTE: the caller is responsible for providing some *specific* [JavaValueProvider]
 *       that can create values for class under test (otherwise it will be mocked),
 *       [ObjectValueProvider] and [NullValueProvider] do not count as *specific*.
 */
fun JavaFuzzingContext.mockAllTypesWithoutSpecificValueProvider() =
    MockingJavaFuzzingContext(delegateContext = this)

class MockingJavaFuzzingContext(
    val delegateContext: JavaFuzzingContext
) : JavaFuzzingContext by delegateContext {
    private val mockValueProvider = MockValueProvider(delegateContext.idGenerator)

    override val valueProvider: JavaValueProvider =
        // NOTE: we first remove `NullValueProvider` from `delegateContext.valueProvider` and then
        //       add it back as a part of our `withFallback` so it has the same priority as
        //       `mockValueProvider`, otherwise mocks will never be used where `null` can be used.
        delegateContext.valueProvider
            .except { it is NullValueProvider }
            .except { it is ObjectValueProvider }
            .withFallback(
                mockValueProvider
                    .with(NullValueProvider)
            )

    override fun handleFuzzedConcreteExecutionResult(concreteExecutionResult: UtConcreteExecutionResult) =
        mockValueProvider.addMockingCandidates(concreteExecutionResult.detectedMockingCandidates)
}